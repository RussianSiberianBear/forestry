package com.alhrb.forestry.service.importer;

import com.alhrb.forestry.model.staging.ForestStandKmlImport;
import com.alhrb.forestry.model.staging.ImportStatus;
import com.alhrb.forestry.repository.staging.ForestStandKmlImportRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForestStandKmlImportService {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final ForestStandKmlImportRepository repository;
    private final EntityManager entityManager;

    private final GeometryFactory geometryFactory = new GeometryFactory(
            new PrecisionModel(),
            4326
    );

    /**
     * Загружает все Placemark из KML во временную таблицу
     * staging.forest_stand_kml_import.
     *
     * @param uploadFileId идентификатор исходного архива в staging.file_storage
     * @param kmlPath      полный путь к распакованному KML-файлу
     * @return результат импорта
     */
    @Transactional
    public KmlImportResult importKml(
            Long uploadFileId,
            Path kmlPath
    ) throws IOException {

        Objects.requireNonNull(
                uploadFileId,
                "uploadFileId не должен быть null"
        );
        Objects.requireNonNull(
                kmlPath,
                "kmlPath не должен быть null"
        );

        Path normalizedPath = kmlPath
                .toAbsolutePath()
                .normalize();

        validateKmlFile(normalizedPath);

        /*
         * Повторная обработка одной загрузки не должна создавать дубли.
         * Репозиторий должен выполнять bulk DELETE через @Modifying @Query.
         */
        int deletedRows = (int) repository.deleteByUploadFileId(uploadFileId);

        repository.flush();
        entityManager.clear();

        if (deletedRows > 0) {
            log.info(
                    "Удалены предыдущие строки KML-импорта: storageId={}, rows={}",
                    uploadFileId,
                    deletedRows
            );
        }

        int total = 0;
        int imported = 0;
        int errors = 0;

        List<ForestStandKmlImport> batch =
                new ArrayList<>(BATCH_SIZE);

        XMLInputFactory factory = createSecureXmlInputFactory();

        try (InputStream input = new BufferedInputStream(
                Files.newInputStream(normalizedPath)
        )) {
            XMLStreamReader reader =
                    factory.createXMLStreamReader(input);

            try {
                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event != XMLStreamConstants.START_ELEMENT) {
                        continue;
                    }

                    if (!"Placemark".equals(reader.getLocalName())) {
                        continue;
                    }

                    total++;

                    PlacemarkData data = readPlacemark(reader);

                    ForestStandKmlImport entity =
                            map(uploadFileId, data);

                    if (entity.getImportStatus() == ImportStatus.ERROR) {
                        errors++;
                    } else {
                        imported++;
                    }

                    batch.add(entity);

                    if (batch.size() >= BATCH_SIZE) {
                        persistBatch(batch);
                    }
                }
            } finally {
                reader.close();
            }

        } catch (XMLStreamException e) {
            throw new IOException(
                    "Некорректный KML/XML: " + e.getMessage(),
                    e
            );
        }

        persistBatch(batch);

        log.info(
                "KML импортирован: storageId={}, file={}, placemarks={}, ok={}, errors={}",
                uploadFileId,
                normalizedPath,
                total,
                imported,
                errors
        );

        return new KmlImportResult(
                uploadFileId,
                total,
                imported,
                errors
        );
    }

    private void validateKmlFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException(
                    "KML-файл не найден: " + path
            );
        }

        String filename = path
                .getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (!filename.endsWith(".kml")) {
            throw new IllegalArgumentException(
                    "Ожидался KML-файл: " + path
            );
        }
    }

    /**
     * Сохраняет одну пачку и освобождает persistence context.
     */
    private void persistBatch(
            List<ForestStandKmlImport> batch
    ) {
        if (batch.isEmpty()) {
            return;
        }

        repository.saveAll(batch);
        repository.flush();

        /*
         * flush() отправляет INSERT в БД, но сущности остаются
         * управляемыми Hibernate. clear() освобождает их после
         * каждой пачки и не даёт persistence context разрастаться.
         */
        entityManager.clear();

        batch.clear();
    }

    private XMLInputFactory createSecureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();

        setPropertyIfSupported(
                factory,
                XMLInputFactory.SUPPORT_DTD,
                false
        );
        setPropertyIfSupported(
                factory,
                "javax.xml.stream.isSupportingExternalEntities",
                false
        );
        setPropertyIfSupported(
                factory,
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );
        setPropertyIfSupported(
                factory,
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        return factory;
    }

    private void setPropertyIfSupported(
            XMLInputFactory factory,
            String key,
            Object value
    ) {
        try {
            factory.setProperty(key, value);
        } catch (IllegalArgumentException ignored) {
            log.debug(
                    "XMLInputFactory не поддерживает свойство {}",
                    key
            );
        }
    }

    private PlacemarkData readPlacemark(
            XMLStreamReader reader
    ) throws XMLStreamException {

        Map<String, String> attributes =
                new LinkedHashMap<>();

        List<PolygonData> polygons =
                new ArrayList<>();

        PolygonData currentPolygon = null;

        boolean inOuterBoundary = false;
        boolean inInnerBoundary = false;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "SimpleData" -> {
                        String name = reader.getAttributeValue(
                                null,
                                "name"
                        );

                        String value = reader.getElementText();

                        if (name != null) {
                            attributes.put(
                                    name,
                                    trimToNull(value)
                            );
                        }
                    }

                    case "Polygon" -> currentPolygon = new PolygonData();

                    case "outerBoundaryIs" -> inOuterBoundary = true;

                    case "innerBoundaryIs" -> inInnerBoundary = true;

                    case "coordinates" -> {
                        String coordinates =
                                reader.getElementText();

                        if (currentPolygon != null) {
                            if (inOuterBoundary) {
                                currentPolygon.outer = coordinates;
                            } else if (inInnerBoundary) {
                                currentPolygon.holes.add(
                                        coordinates
                                );
                            }
                        }
                    }

                    default -> {
                    }
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "outerBoundaryIs" -> inOuterBoundary = false;

                    case "innerBoundaryIs" -> inInnerBoundary = false;

                    case "Polygon" -> {
                        if (currentPolygon != null) {
                            polygons.add(currentPolygon);
                            currentPolygon = null;
                        }
                    }

                    case "Placemark" -> {
                        return new PlacemarkData(
                                attributes,
                                polygons
                        );
                    }

                    default -> {
                    }
                }
            }
        }

        return new PlacemarkData(
                attributes,
                polygons
        );
    }

    private ForestStandKmlImport map(
            Long uploadFileId,
            PlacemarkData data
    ) {
        ForestStandKmlImport entity =
                new ForestStandKmlImport();

        entity.setUploadFileId(uploadFileId);

        Map<String, String> attributes =
                data.attributes();

        entity.setMk(toInteger(attributes.get("MK")));
        entity.setZk(toInteger(attributes.get("ZK")));
        entity.setVmr(attributes.get("VMR"));
        entity.setBon(toInteger(attributes.get("BON")));
        entity.setMtip(attributes.get("MTIP"));
        entity.setStrata(toInteger(attributes.get("STRATA")));
        entity.setUgir1(attributes.get("UGIR_1"));

        entity.setInd(attributes.get("Ind"));
//        entity.setInd(attributes.get("лесни") + "_" + attributes.get("кварт") + "_" + attributes.get("выдел"));

        entity.setLesho(attributes.get("лесхо"));
        entity.setLesni(attributes.get("лесни"));
        entity.setGrupp(attributes.get("групп"));
        entity.setKateg(attributes.get("катег"));
        entity.setOzu(attributes.get("ОЗУ"));

        entity.setKvart(toInteger(attributes.get("кварт")));
        entity.setVydel(attributes.get("выдел"));

        if (attributes.get("кварт") != null && toInteger(attributes.get("кварт")) != 0 && attributes.get("выдел") != null) {
            entity.setInd(attributes.get("лесни") + "_" + attributes.get("кварт") + "_" + attributes.get("выдел"));
        } else {
            String[] parts = null;
            if (attributes.get("Ind") != null && !attributes.get("Ind").isBlank()) {
                parts = attributes.get("Ind").split("_");
            }
            if (attributes.get("кварт") == null || toInteger(attributes.get("кварт")) == 0) {
                if (parts[1] != null) entity.setKvart(toInteger(parts[1]));
            } else {
                entity.setKvart(toInteger(attributes.get("кварт")));
            }

            if (attributes.get("выдел") == null || attributes.get("выдел").isBlank()) {
                if (parts[2] != null) entity.setVydel(parts[2]);
            } else {
                entity.setVydel(attributes.get("выдел"));
            }
        }
  //      entity.setIndexField(attributes.get("Index"));
        entity.setIndexField(entity.getInd());

        if (attributes.get("площа") != null)
            entity.setPlosha(attributes.get("площа").replace(',', '.'));
        else entity.setPlosha(attributes.get("площа"));

        entity.setKate1(attributes.get("кате_1"));
        entity.setSosta(attributes.get("соста"));
        entity.setPreob(attributes.get("преоб"));
        entity.setVozra(toInteger(attributes.get("возра")));
        entity.setVysot(toBigDecimal(attributes.get("высот")));
        entity.setDiame(toBigDecimal(attributes.get("диаме")));
        entity.setKlass(toBigDecimal(attributes.get("класс")));
        entity.setBonit(attributes.get("бонит"));
        entity.setTipL(attributes.get("тип_л"));
        entity.setTlu(attributes.get("тлу"));
        entity.setPolno(attributes.get("полно"));
        entity.setZapas(toBigDecimal(attributes.get("запас")));

        entity.setMu(attributes.get("Mu"));
        entity.setGir(attributes.get("Gir"));
        entity.setUgir(attributes.get("Ugir"));
        if ((attributes.get("Y") != null)) {
            entity.setY(attributes.get("Y").replace(',', '.'));
        } else {
            entity.setY(attributes.get("Y"));
        }

        if ((attributes.get("X") != null)) {
            entity.setX(attributes.get("X").replace(',', '.'));
        } else {
            entity.setX(attributes.get("X"));
        }

        String rawCoordinates = data.polygons()
                .stream()
                .map(polygon -> polygon.outer)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");

        entity.setCoordinates(rawCoordinates);

        try {
            Geometry geometry =
                    createGeometry(data.polygons());

            if (geometry == null || geometry.isEmpty()) {
                throw new IllegalArgumentException(
                        "В Placemark отсутствует Polygon с координатами"
                );
            }

            entity.setGeometry(geometry);
            entity.setImportStatus(ImportStatus.PENDING);
            entity.setErrorMessage(null);

        } catch (Exception e) {
            entity.setGeometry(null);
            entity.setImportStatus(ImportStatus.ERROR);
            entity.setErrorMessage(
                    limit(
                            e.getMessage(),
                            MAX_ERROR_MESSAGE_LENGTH
                    )
            );
        }

        return entity;
    }

    private Geometry createGeometry(
            List<PolygonData> polygonDataList
    ) {
        List<Polygon> polygons = new ArrayList<>();

        for (PolygonData polygonData : polygonDataList) {
            if (trimToNull(polygonData.outer) == null) {
                continue;
            }

            LinearRing shell = geometryFactory.createLinearRing(
                    parseCoordinates(polygonData.outer)
            );

            LinearRing[] holes = polygonData.holes
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::parseCoordinates)
                    .map(geometryFactory::createLinearRing)
                    .toArray(LinearRing[]::new);

            Polygon polygon = geometryFactory.createPolygon(
                    shell,
                    holes
            );

            polygon.setSRID(4326);
            polygons.add(polygon);
        }

        if (polygons.isEmpty()) {
            return null;
        }

        if (polygons.size() == 1) {
            return polygons.getFirst();
        }

        MultiPolygon multiPolygon =
                geometryFactory.createMultiPolygon(
                        polygons.toArray(Polygon[]::new)
                );

        multiPolygon.setSRID(4326);

        return multiPolygon;
    }

    private Coordinate[] parseCoordinates(String source) {
        String normalized = source == null
                ? ""
                : source.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Пустой список координат"
            );
        }

        String[] tuples = normalized.split("\\s+");

        List<Coordinate> coordinates =
                new ArrayList<>(tuples.length + 1);

        for (String tuple : tuples) {
            if (tuple.isBlank()) {
                continue;
            }

            String[] parts = tuple.split(",");

            if (parts.length < 2) {
                throw new IllegalArgumentException(
                        "Некорректная координата: " + tuple
                );
            }

            double longitude =
                    Double.parseDouble(parts[0].trim());

            double latitude =
                    Double.parseDouble(parts[1].trim());

            coordinates.add(
                    new Coordinate(
                            longitude,
                            latitude
                    )
            );
        }

        if (coordinates.size() < 3) {
            throw new IllegalArgumentException(
                    "Для полигона недостаточно координат"
            );
        }

        Coordinate first = coordinates.getFirst();
        Coordinate last = coordinates.getLast();

        if (!first.equals2D(last)) {
            coordinates.add(
                    new Coordinate(first)
            );
        }

        if (coordinates.size() < 4) {
            throw new IllegalArgumentException(
                    "Линейное кольцо должно содержать "
                            + "минимум четыре точки с замыканием"
            );
        }

        return coordinates.toArray(Coordinate[]::new);
    }

    private Integer toInteger(String value) {
        BigDecimal decimal = toBigDecimal(value);

        return decimal == null
                ? null
                : decimal.intValueExact();
    }

    private BigDecimal toBigDecimal(String value) {
        String normalized = trimToNull(value);

        if (normalized == null) {
            return null;
        }

        return new BigDecimal(
                normalized.replace(',', '.')
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private String limit(
            String value,
            int maxLength
    ) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private record PlacemarkData(
            Map<String, String> attributes,
            List<PolygonData> polygons
    ) {
    }

    private static final class PolygonData {

        private String outer;

        private final List<String> holes =
                new ArrayList<>();
    }
}
