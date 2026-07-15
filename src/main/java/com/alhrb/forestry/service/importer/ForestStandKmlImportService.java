package com.alhrb.forestry.service.importer;

import com.alhrb.forestry.model.staging.ForestStandKmlImport;
import com.alhrb.forestry.repository.staging.ForestStandKmlImportRepository;
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

    private final ForestStandKmlImportRepository repository;

    private final GeometryFactory geometryFactory = new GeometryFactory(
            new PrecisionModel(),
            4326
    );

    /**
     * Загружает все Placemark из KML во временную таблицу staging.forest_stand_kml_import.
     * uploadFileId — идентификатор исходного архива в staging.file_storage.
     */
    @Transactional
    public KmlImportResult importKml(Long uploadFileId, Path kmlPath) throws IOException {
        Objects.requireNonNull(uploadFileId, "uploadFileId не должен быть null");
        Objects.requireNonNull(kmlPath, "kmlPath не должен быть null");

        Path normalizedPath = kmlPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedPath)) {
            throw new IOException("KML-файл не найден: " + normalizedPath);
        }
        if (!normalizedPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".kml")) {
            throw new IllegalArgumentException("Ожидался KML-файл: " + normalizedPath);
        }

        // Повторная обработка одной загрузки не создаёт дубли во временной таблице.
        repository.deleteByUploadFileId(uploadFileId);

        int total = 0;
        int imported = 0;
        int errors = 0;
        List<ForestStandKmlImport> batch = new ArrayList<>(BATCH_SIZE);

        XMLInputFactory factory = createSecureXmlInputFactory();

        try (InputStream input = new BufferedInputStream(Files.newInputStream(normalizedPath))) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.START_ELEMENT
                            && "Placemark".equals(reader.getLocalName())) {
                        total++;
                        PlacemarkData data = readPlacemark(reader);
                        ForestStandKmlImport entity = map(uploadFileId, data);
                        if ("ERROR".equals(entity.getImportStatus())) {
                            errors++;
                        } else {
                            imported++;
                        }
                        batch.add(entity);

                        if (batch.size() >= BATCH_SIZE) {
                            repository.saveAllAndFlush(batch);
                            batch.clear();
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException("Некорректный KML/XML: " + e.getMessage(), e);
        }

        if (!batch.isEmpty()) {
            repository.saveAllAndFlush(batch);
        }

        log.info("KML импортирован: storageId={}, file={}, placemarks={}, ok={}, errors={}",
                uploadFileId, normalizedPath, total, imported, errors);

        return new KmlImportResult(uploadFileId, total, imported, errors);
    }

    private XMLInputFactory createSecureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        setPropertyIfSupported(factory, XMLInputFactory.SUPPORT_DTD, false);
        setPropertyIfSupported(factory, "javax.xml.stream.isSupportingExternalEntities", false);
        setPropertyIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setPropertyIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private void setPropertyIfSupported(XMLInputFactory factory, String key, Object value) {
        try {
            factory.setProperty(key, value);
        } catch (IllegalArgumentException ignored) {
            log.debug("XMLInputFactory не поддерживает свойство {}", key);
        }
    }

    private PlacemarkData readPlacemark(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> attributes = new LinkedHashMap<>();
        List<PolygonData> polygons = new ArrayList<>();
        PolygonData currentPolygon = null;
        boolean inOuterBoundary = false;
        boolean inInnerBoundary = false;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                switch (localName) {
                    case "SimpleData" -> {
                        String name = reader.getAttributeValue(null, "name");
                        String value = reader.getElementText();
                        if (name != null) {
                            attributes.put(name, trimToNull(value));
                        }
                    }
                    case "Polygon" -> currentPolygon = new PolygonData();
                    case "outerBoundaryIs" -> inOuterBoundary = true;
                    case "innerBoundaryIs" -> inInnerBoundary = true;
                    case "coordinates" -> {
                        String coordinates = reader.getElementText();
                        if (currentPolygon != null) {
                            if (inOuterBoundary) {
                                currentPolygon.outer = coordinates;
                            } else if (inInnerBoundary) {
                                currentPolygon.holes.add(coordinates);
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
                        return new PlacemarkData(attributes, polygons);
                    }
                    default -> {
                    }
                }
            }
        }

        return new PlacemarkData(attributes, polygons);
    }

    private ForestStandKmlImport map(Long uploadFileId, PlacemarkData data) {
        ForestStandKmlImport entity = new ForestStandKmlImport();
        entity.setUploadFileId(uploadFileId);

        Map<String, String> a = data.attributes;
        entity.setMk(toInteger(a.get("MK")));
        entity.setZk(toInteger(a.get("ZK")));
        entity.setVmr(a.get("VMR"));
        entity.setBon(toInteger(a.get("BON")));
        entity.setMtip(a.get("MTIP"));
        entity.setStrata(toInteger(a.get("STRATA")));
        entity.setUgir1(a.get("UGIR_1"));
        entity.setInd(a.get("Ind"));
        entity.setLesho(a.get("лесхо"));
        entity.setLesni(a.get("лесни"));
        entity.setGrupp(a.get("групп"));
        entity.setKateg(a.get("катег"));
        entity.setOzu(a.get("ОЗУ"));
        entity.setKvart(toBigDecimal(a.get("кварт")));
        entity.setVydel(a.get("выдел"));
        entity.setPlosha(a.get("площа"));
        entity.setKate1(a.get("кате_1"));
        entity.setSosta(a.get("соста"));
        entity.setPreob(a.get("преоб"));
        entity.setVozra(toBigDecimal(a.get("возра")));
        entity.setVysot(toBigDecimal(a.get("высот")));
        entity.setDiame(toBigDecimal(a.get("диаме")));
        entity.setKlass(toBigDecimal(a.get("класс")));
        entity.setBonit(a.get("бонит"));
        entity.setTipL(a.get("тип_л"));
        entity.setTlu(a.get("тлу"));
        entity.setPolno(a.get("полно"));
        entity.setZapas(toBigDecimal(a.get("запас")));
        entity.setIndexField(a.get("Index"));
        entity.setMu(a.get("Mu"));
        entity.setGir(a.get("Gir"));
        entity.setUgir(a.get("Ugir"));
        entity.setY(a.get("Y"));
        entity.setX(a.get("X"));

        String rawCoordinates = data.polygons.stream()
                .map(p -> p.outer)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
        entity.setCoordinates(rawCoordinates);

        try {
            Geometry geometry = createGeometry(data.polygons);
            if (geometry == null || geometry.isEmpty()) {
                throw new IllegalArgumentException("В Placemark отсутствует Polygon с координатами");
            }
            entity.setGeometry(geometry);
            entity.setImportStatus("PENDING");
        } catch (Exception e) {
            entity.setGeometry(null);
            entity.setImportStatus("ERROR");
            entity.setErrorMessage(limit(e.getMessage(), 2000));
        }

        return entity;
    }

    private Geometry createGeometry(List<PolygonData> polygonDataList) {
        List<Polygon> polygons = new ArrayList<>();
        for (PolygonData data : polygonDataList) {
            if (trimToNull(data.outer) == null) {
                continue;
            }
            LinearRing shell = geometryFactory.createLinearRing(parseCoordinates(data.outer));
            LinearRing[] holes = data.holes.stream()
                    .filter(Objects::nonNull)
                    .map(this::parseCoordinates)
                    .map(geometryFactory::createLinearRing)
                    .toArray(LinearRing[]::new);
            Polygon polygon = geometryFactory.createPolygon(shell, holes);
            polygon.setSRID(4326);
            polygons.add(polygon);
        }

        if (polygons.isEmpty()) {
            return null;
        }
        if (polygons.size() == 1) {
            return polygons.getFirst();
        }
        MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(polygons.toArray(Polygon[]::new));
        multiPolygon.setSRID(4326);
        return multiPolygon;
    }

    private Coordinate[] parseCoordinates(String source) {
        String normalized = source == null ? "" : source.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Пустой список координат");
        }

        String[] tuples = normalized.split("\\s+");
        List<Coordinate> coordinates = new ArrayList<>(tuples.length + 1);
        for (String tuple : tuples) {
            if (tuple.isBlank()) {
                continue;
            }
            String[] parts = tuple.split(",");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Некорректная координата: " + tuple);
            }
            double longitude = Double.parseDouble(parts[0].trim());
            double latitude = Double.parseDouble(parts[1].trim());
            coordinates.add(new Coordinate(longitude, latitude));
        }

        if (coordinates.size() < 3) {
            throw new IllegalArgumentException("Для полигона недостаточно координат");
        }

        Coordinate first = coordinates.getFirst();
        Coordinate last = coordinates.getLast();
        if (!first.equals2D(last)) {
            coordinates.add(new Coordinate(first));
        }

        if (coordinates.size() < 4) {
            throw new IllegalArgumentException("Линейное кольцо должно содержать минимум четыре точки с замыканием");
        }

        return coordinates.toArray(Coordinate[]::new);
    }

    private Integer toInteger(String value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.intValueExact();
    }

    private BigDecimal toBigDecimal(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return new BigDecimal(normalized.replace(',', '.'));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int maxLength) {
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
        private final List<String> holes = new ArrayList<>();
    }
}
