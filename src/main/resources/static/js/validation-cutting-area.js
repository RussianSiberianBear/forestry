// ==========================================
// ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ
// ==========================================

let showLabels = true;
let cachedPlots = null;
let polygonLayer = null;
let labelLayer = null;
let mapElement='map';

// ==========================================
// ФИЛЬТРЫ ДЛЯ КАРТЫ
// ==========================================

let currentFilters = {};

function collectFilters() {
    if (typeof collectFilterAuto === 'function') {
        const filters = collectFilterAuto('plotForm', {
            selector: '[data-filter]',
            trim: true,
            skipEmpty: true,
            skipDisabled: true
        });
        console.log('📋 Собранные фильтры (авто):', filters);
        return filters;
    } else {
        console.warn('⚠️ Функция collectFilterAuto не найдена, используем ручной сбор');
        return collectFiltersManual();
    }
}

function collectFiltersManual() {
    const forestrySelect = document.getElementById('forestrySelect');
    const subForestrySelect = document.getElementById('subForestrySelect');
    const technicalUnitSelect = document.getElementById('technicalUnitSelect');
    const quarterId = document.getElementById('quarterId');
    const cutTypeSelect = document.getElementById('cutType');
    const yearOfCutInput = document.getElementById('yearOfCut');
    const numberInQuarterInput = document.getElementById('numberInQuarter');

    const filters = {};

    const fId = forestrySelect?.value;
    if (fId && fId !== '') filters.forestryId = fId;

    const dfId = subForestrySelect?.value;
    if (dfId && dfId !== '') filters.subForestryId = dfId;

    const tId = technicalUnitSelect?.value;
    if (tId && tId !== '') filters.technicalUnitId = tId;

    const qId = quarterId?.value;
    if (qId && qId !== '') filters.quarterId = qId;

    const cutType = cutTypeSelect?.value;
    if (cutType && cutType !== '') filters.cutType = cutType;

    const yearOfCut = yearOfCutInput?.value;
    if (yearOfCut && yearOfCut !== '') filters.yearOfCut = yearOfCut;

    const numberInQuarter = numberInQuarterInput?.value?.trim();
    if (numberInQuarter && numberInQuarter !== '') filters.numberInQuarter = numberInQuarter;

    console.log('📋 Собранные фильтры (ручной):', filters);
    return filters;
}

// ==========================================
// ОБНОВЛЕНИЕ КАРТЫ
// ==========================================

function refreshMap() {
    currentFilters = collectFilters();
    console.log('🔍 Обновление карты с фильтрами:', currentFilters);

    const mapContainer = document.getElementById(mapElement);
    if (mapContainer) {
        mapContainer.style.opacity = '0.6';
    }

    const params = new URLSearchParams();
    Object.keys(currentFilters).forEach(key => {
        params.append(key, currentFilters[key]);
    });

    const hasFilters = Object.keys(currentFilters).length > 0;
    const url = hasFilters
        ? '/api/cutting-area/map-data-filtered?' + params.toString()
        : '/api/cutting-area/map-data';

    console.log('📡 Запрос к:', url);

    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(plots => {
            cachedPlots = plots;
            renderPlots(plots);

            const count = plots ? plots.length : 0;

            const infoSpan = document.getElementById('filterInfo');
            if (infoSpan) {
                if (Object.keys(currentFilters).length === 0) {
                    infoSpan.textContent = 'Все деляны';
                    infoSpan.style.background = '#1e87f0';
                } else {
                    let filterText = `Найдено: ${count}`;
                    if (plots && plots.length > 0) {
                        const numbers = plots
                            .map(p => p.numberInQuarter)
                            .filter(n => n && n !== '')
                            .sort((a, b) => {
                                const numA = parseFloat(a);
                                const numB = parseFloat(b);
                                if (!isNaN(numA) && !isNaN(numB)) {
                                    return numA - numB;
                                }
                                return a.localeCompare(b);
                            });
                        if (numbers.length > 0) {
                            const displayNumbers = numbers.length > 10
                                ? numbers.slice(0, 10).join(', ') + `... +${numbers.length - 10}`
                                : numbers.join(', ');
                            filterText += ` (Дел. №: ${displayNumbers})`;
                        }
                    }
                    infoSpan.textContent = filterText;
                    infoSpan.style.background = '#2e7d32';
                }
            }

            updateLegend(plots);

            if (mapContainer) {
                mapContainer.style.opacity = '1';
            }

            UIkit.notification({
                message: `🗺️ Карта обновлена. Загружено ${count} делян`,
                status: 'success',
                timeout: 2000
            });
        })
        .catch(error => {
            console.error('❌ Ошибка загрузки делян:', error);
            if (mapContainer) {
                mapContainer.style.opacity = '1';
            }
            UIkit.notification({
                message: '❌ Ошибка загрузки: ' + error.message,
                status: 'danger',
                timeout: 3000
            });
        });
}

// ==========================================
// ЛЕГЕНДА КАРТЫ
// ==========================================

function updateLegend(plotsData) {
    const oldLegend = document.querySelector('.custom-legend');
    if (oldLegend) {
        oldLegend.remove();
    }

    const legend = document.createElement('div');
    legend.className = 'custom-legend';
    legend.style.cssText = `
        position: absolute;
        bottom: 30px;
        right: 10px;
        background: rgba(255,255,255,0.92);
        padding: 10px 14px;
        border-radius: 6px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        z-index: 1000;
        font-size: 12px;
        font-family: 'Segoe UI', Arial, sans-serif;
        border: 1px solid #ddd;
        pointer-events: none;
        max-width: 220px;
        `;

    const labelsStatus = showLabels ? '🟢 Включены' : '🔴 Выключены';

    let filterInfo = '';
    const filters = collectFilters();
    if (filters.cutType) {
        filterInfo += `<div style="font-size: 10px; color: #1e87f0;">Тип рубки: ${filters.cutType}</div>`;
    }
    if (filters.yearOfCut) {
        filterInfo += `<div style="font-size: 10px; color: #1e87f0;">Год рубки: ${filters.yearOfCut}</div>`;
    }

    if (plotsData && Array.isArray(plotsData) && plotsData.length > 0) {
        const numbers = plotsData
            .map(p => p.numberInQuarter)
            .filter(n => n !== undefined && n !== null && n !== '')
            .sort((a, b) => {
                const numA = parseFloat(a);
                const numB = parseFloat(b);
                if (!isNaN(numA) && !isNaN(numB)) {
                    return numA - numB;
                }
                return a.localeCompare(b);
            });
        if (numbers.length > 0) {
            const displayNumbers = numbers.length > 10
                ? numbers.slice(0, 10).join(', ') + `... +${numbers.length - 10}`
                : numbers.join(', ');
            filterInfo += `<div style="font-size: 10px; color: #1e87f0;">Дел. №: ${displayNumbers}</div>`;
        }
    }

    legend.innerHTML = `
        <div style="font-weight: bold; margin-bottom: 4px; color: #333;">📋 Информация на карте:</div>
        <div style="display: flex; flex-direction: column; gap: 2px;">
            <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 12px; height: 12px; background: #d32f2f; border-radius: 2px; opacity: 0.3;"></span>
                <span style="color: #555;">Красные полигоны — деляны</span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 12px; height: 12px; background: #d32f2f; border-radius: 50%; border: 1px solid #d32f2f;"></span>
                <span style="color: #555;">Метки: Квартал / Деляна / Площадь</span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px; margin-top: 2px; border-top: 1px solid #eee; padding-top: 4px;">
                <span style="font-size: 10px; color: #999;">Метки: ${labelsStatus}</span>
                <span style="font-size: 10px; color: #999;">| При наведении — увеличение</span>
            </div>
            ${filterInfo ? `<div style="border-top: 1px solid #eee; padding-top: 4px; margin-top: 2px;">${filterInfo}</div>` : ''}
        </div>
    `;

    const mapContainer = document.getElementById(mapElement);
    if (mapContainer) {
        mapContainer.style.position = 'relative';
        mapContainer.appendChild(legend);
    }
}

function loadAllPlots() {
    refreshMap();
}

// ==========================================
// КООРДИНАТЫ
// ==========================================

function handleCoordInput(event, field, index) {
    if (event.key === 'Enter') {
        event.preventDefault();

        if (field === 'lat') {
            const row = document.getElementById('coord-row-' + index);
            if (row) {
                const lngInput = row.querySelector('input[placeholder="Долгота"]');
                if (lngInput) lngInput.focus();
            }
        } else if (field === 'lng') {
            addNewCoordinateRow();
        }
    }
}

function addNewCoordinateRow() {
    const container = document.getElementById('coordinates-container');
    const index = container.children.length;

    if (index > 0) {
        const lastRow = container.children[index - 1];
        const latInput = lastRow.querySelector('input[placeholder="Широта"]');
        const lngInput = lastRow.querySelector('input[placeholder="Долгота"]');

        if (latInput && lngInput) {
            const lat = parseFloat(latInput.value.replace(',', '.').trim());
            const lng = parseFloat(lngInput.value.replace(',', '.').trim());

            if (isNaN(lat) || isNaN(lng)) {
                UIkit.notification({
                    message: '❌ Заполните оба поля: Широту и Долготу',
                    status: 'warning',
                    timeout: 2000
                });
                return;
            }

            if (lat < -90 || lat > 90) {
                UIkit.notification({
                    message: '❌ Широта должна быть в диапазоне -90...90',
                    status: 'warning',
                    timeout: 2000
                });
                return;
            }

            if (lng < -180 || lng > 180) {
                UIkit.notification({
                    message: '❌ Долгота должна быть в диапазоне -180...180',
                    status: 'warning',
                    timeout: 2000
                });
                return;
            }
        }
    }

    const row = document.createElement('div');
    row.className = 'coordinate-row';
    row.id = 'coord-row-' + index;
    row.innerHTML = `
        <input class="uk-input uk-form-width-small coord-input" type="text"
               name="coordinates[${index}].lat"
               placeholder="Широта"
               onkeydown="handleCoordInput(event, 'lat', ${index})"
               oninput="validateCoordInput(this)">
        <input class="uk-input uk-form-width-small coord-input" type="text"
               name="coordinates[${index}].lng"
               placeholder="Долгота"
               onkeydown="handleCoordInput(event, 'lng', ${index})"
               oninput="validateCoordInput(this)">
        <button type="button" class="uk-button uk-button-danger uk-button-small"
                onclick="removeCoordinate(${index})">
            <span uk-icon="icon: close"></span>
        </button>
    `;
    container.appendChild(row);

    const newLatInput = row.querySelector('input[placeholder="Широта"]');
    if (newLatInput) newLatInput.focus();

    updateCoordCounter();
    updateIndices();
}

function removeCoordinate(index) {
    const container = document.getElementById('coordinates-container');
    const count = container.children.length;

    if (count <= 3) {
        UIkit.notification({
            message: '❌ Нельзя удалить последнюю точку (нужно минимум 3)',
            status: 'warning',
            timeout: 2000
        });
        return;
    }

    const element = document.getElementById('coord-row-' + index);
    if (element) {
        element.remove();
        updateIndices();
        updateCoordCounter();

        UIkit.notification({
            message: '✅ Точка удалена',
            status: 'success',
            timeout: 1000
        });
    }
}

function clearAllCoordinates() {
    const container = document.getElementById('coordinates-container');
    const count = container.children.length;

    if (count <= 3) {
        UIkit.notification({
            message: '❌ Нужно минимум 3 точки',
            status: 'warning',
            timeout: 2000
        });
        return;
    }

    while (container.children.length > 3) {
        container.removeChild(container.lastChild);
    }
    updateIndices();
    updateCoordCounter();

    container.querySelectorAll('input[type="text"]').forEach(input => input.value = '');

    const firstRow = container.children[0];
    if (firstRow) {
        const latInput = firstRow.querySelector('input[placeholder="Широта"]');
        if (latInput) latInput.focus();
    }

    UIkit.notification({
        message: '✅ Очищено до 3 точек',
        status: 'success',
        timeout: 1500
    });
}

function updateIndices() {
    const rows = document.querySelectorAll('#coordinates-container .coordinate-row');
    rows.forEach((row, index) => {
        row.id = 'coord-row-' + index;

        const latInput = row.querySelector('input[placeholder="Широта"]');
        const lngInput = row.querySelector('input[placeholder="Долгота"]');
        if (latInput) {
            latInput.name = 'coordinates[' + index + '].lat';
            latInput.setAttribute('onkeydown', 'handleCoordInput(event, \'lat\', ' + index + ')');
        }
        if (lngInput) {
            lngInput.name = 'coordinates[' + index + '].lng';
            lngInput.setAttribute('onkeydown', 'handleCoordInput(event, \'lng\', ' + index + ')');
        }

        const btn = row.querySelector('button');
        if (btn) {
            btn.setAttribute('onclick', 'removeCoordinate(' + index + ')');
        }
    });
}

function updateCoordCounter() {
    const container = document.getElementById('coordinates-container');
    const count = container.children.length;
    const counter = document.getElementById('coordCounter');
    if (counter) {
        counter.textContent = 'Точек: ' + count;
    }
}

function validateCoordInput(input) {
    input.value = input.value.replace(',', '.').trim();
    if (input.value !== '' && isNaN(parseFloat(input.value))) {
        input.style.borderColor = '#f0506e';
    } else {
        input.style.borderColor = '';
    }
}

// ==========================================
// СБРОС ФОРМЫ
// ==========================================

function resetForm() {
    const container = document.getElementById('coordinates-container');
    while (container.children.length > 3) {
        container.removeChild(container.lastChild);
    }
    container.querySelectorAll('input[type="text"]').forEach(input => input.value = '');
    updateIndices();
    updateCoordCounter();

    document.getElementById('numberInQuarter').value = '';
    document.getElementById('plots').value = '';
    document.getElementById('description').value = '';
    document.getElementById('yearOfCut').value = '';
    document.getElementById('cutType').value = '';

    resetAllDependentSelects();
}

// ==========================================
// НАСТРОЙКИ UI
// ==========================================

function saveUISetting(endpoint, value) {
    fetch('/api/ui-settings/' + endpoint + '/' + value, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.text();
        })
        .then(() => {
            console.log('✅ Настройка сохранена:', endpoint, value);
        })
        .catch(error => console.error('❌ Ошибка сохранения настройки:', error));
}

// ==========================================
// ЗАГРУЗКА НАСТРОЕК
// ==========================================

function loadUISettingsFromServer() {
    const forestryUnitId = document.getElementById('uiForestryUnitId')?.value;
    const forestryType = document.getElementById('uiForestryType')?.value;
    const centerLat = document.getElementById('uiCenterLat')?.value;
    const centerLng = document.getElementById('uiCenterLng')?.value;
    const zoom = document.getElementById('uiZoom')?.value;
    const cutType = document.getElementById('uiCutType')?.value;
    const yearOfCut = document.getElementById('uiYearOfCut')?.value;

    console.log('📥 Загружены настройки:', {
        forestryUnitId, forestryType, cutType, yearOfCut
    });

    if (centerLat && centerLng && map) {
        map.setView([parseFloat(centerLat), parseFloat(centerLng)], parseInt(zoom) || 7);
    }

    if (cutType && cutType !== '') {
        const cutTypeSelect = document.getElementById('cutType');
        if (cutTypeSelect) {
            cutTypeSelect.value = cutType;
        }
    }

    if (yearOfCut && yearOfCut !== '') {
        const yearInput = document.getElementById('yearOfCut');
        if (yearInput) {
            yearInput.value = yearOfCut;
        }
    }

    if (!forestryUnitId || forestryUnitId === '') {
        console.log('⚠️ Нет сохранённого лесничества');
        refreshMap();
        return;
    }

    loadForestryHierarchy(forestryUnitId, forestryType);
}

function loadForestryHierarchy(unitId, type) {
    fetch('/api/territory/' + unitId)
        .then(response => response.json())
        .then(unit => {
            loadFullHierarchy(unit);
        })
        .catch(error => {
            console.error('Ошибка загрузки территории:', error);
            refreshMap();
        });
}

function loadFullHierarchy(unit) {
    const savedId = document.getElementById('uiForestryUnitId')?.value;
    if (savedId) {
        fetch('/api/territory/path/' + savedId)
            .then(response => response.json())
            .then(path => {
                let forestryId = null;
                let districtForestryId = null;
                let technicalUnitId = null;
                let quarterId = null;

                path.forEach(item => {
                    switch (item.type) {
                        case 'FORESTRY':
                            forestryId = item.id;
                            break;
                        case 'SUB_FORESTRY':
                            districtForestryId = item.id;
                            break;
                        case 'TECHNICAL_UNIT':
                            technicalUnitId = item.id;
                            break;
                        case 'QUARTER':
                            quarterId = item.id;
                            break;
                    }
                });

                restoreHierarchy(forestryId, districtForestryId, technicalUnitId, quarterId);
            })
            .catch(error => {
                console.error('Ошибка загрузки пути:', error);
                refreshMap();
            });
    }
}

function restoreHierarchy(forestryId, districtForestryId, technicalUnitId, quarterId) {
    const forestrySelect = document.getElementById('forestrySelect');
    if (forestrySelect && forestryId) {
        forestrySelect.value = forestryId;
        enableQuarterField();
        loadSubForestries(forestryId, function() {
            const districtSelect = document.getElementById('subForestrySelect');
            if (districtSelect && districtForestryId) {
                districtSelect.value = districtForestryId;
                loadTechnicalUnits(districtForestryId, function() {
                    const techSelect = document.getElementById('technicalUnitSelect');
                    if (techSelect && technicalUnitId) {
                        techSelect.value = technicalUnitId;
                    }
                    setQuarter(quarterId);
                });
            } else {
                setQuarter(quarterId);
            }
        });
    }
}

function enableQuarterField() {
    const quarterInput = document.getElementById('quarterInput');
    if (quarterInput) {
        quarterInput.disabled = false;
        quarterInput.placeholder = 'Введите номер квартала...';
    }
    const numberInQuarterInput = document.getElementById('numberInQuarter');
    if (numberInQuarterInput) {
        numberInQuarterInput.disabled = true;
        numberInQuarterInput.placeholder = 'Сначала выберите квартал';
    }
}

function updateNumberInQuarterField() {
    const quarterId = document.getElementById('quarterId')?.value;
    const numberInQuarterInput = document.getElementById('numberInQuarter');

    if (quarterId && quarterId !== '') {
        numberInQuarterInput.disabled = false;
        numberInQuarterInput.placeholder = 'Введите номер деляны...';
    } else {
        numberInQuarterInput.disabled = true;
        numberInQuarterInput.value = '';
        numberInQuarterInput.placeholder = 'Сначала выберите квартал';
    }
}

function setQuarter(quarterId) {
    if (quarterId) {
        fetch('/api/territory/' + quarterId)
            .then(response => response.json())
            .then(unit => {
                if (unit && unit.number) {
                    const input = document.getElementById('quarterInput');
                    if (input) {
                        input.value = 'Кв. ' + unit.number + (unit.name ? ' (' + unit.name + ')' : '');
                        input.disabled = false;
                    }
                    document.getElementById('quarterId').value = quarterId;
                    console.log('✅ Установлен квартал:', unit.number);
                    updateNumberInQuarterField();
                }
            })
            .catch(error => console.error('Ошибка загрузки квартала:', error));
    }
}

// ==========================================
// ПРОВЕРКА ВСЕХ ДЕЛЯН
// ==========================================

function checkIntersectAll() {
    UIkit.notification({
        message: '🔍 Запуск глобальной проверки всех делян...',
        status: 'primary',
        timeout: 3000
    });

    fetch('/api/cutting-area/validate-all', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(conflicts => {
            UIkit.notification.closeAll();

            if (conflicts.length === 0) {
                UIkit.notification({
                    message: '✅ Все деляны проверены! Пересечений не обнаружено.',
                    status: 'success',
                    timeout: 5000
                });

                const conflictBlock = document.getElementById('conflictResults');
                if (conflictBlock) {
                    conflictBlock.style.display = 'none';
                }
            } else {
                UIkit.notification({
                    message: '⚠️ Найдено ' + conflicts.length + ' пересечений! Проверьте список ниже.',
                    status: 'warning',
                    timeout: 5000
                });

                showConflicts(conflicts);
            }
        })
        .catch(error => {
            console.error('Ошибка при проверке:', error);
            UIkit.notification({
                message: '❌ Ошибка при проверке: ' + error.message,
                status: 'danger',
                timeout: 5000
            });
        });
}

// ==========================================
// ОТОБРАЖЕНИЕ КОНФЛИКТОВ
// ==========================================

function showConflicts(conflicts) {
    let conflictBlock = document.getElementById('conflictResults');

    if (!conflictBlock) {
        conflictBlock = document.createElement('div');
        conflictBlock.id = 'conflictResults';
        conflictBlock.className = 'uk-card uk-card-default uk-card-body uk-margin';
        conflictBlock.style.borderLeft = '4px solid #f57c00';

        const header = document.querySelector('.uk-heading-divider');
        if (header) {
            header.parentNode.insertBefore(conflictBlock, header.nextSibling);
        }
    }

    let html = `
        <h3 class="uk-card-title" style="color: #f57c00;">
            <span uk-icon="icon: warning; ratio: 1.2"></span>
            Обнаружены пересечения!
            <span class="uk-badge uk-badge-danger uk-margin-left">${conflicts.length} конф.</span>
        </h3>
        <div class="conflict-list">
            <table class="uk-table uk-table-striped uk-table-hover uk-table-small">
                <thead>
                    <tr>
                        <th>Деляна 1</th>
                        <th>Деляна 2</th>
                        <th>Площадь (м²)</th>
                        <th>Серьёзность</th>
                    </tr>
                </thead>
                <tbody>
    `;

    conflicts.forEach(conflict => {
        const severityClass = conflict.severity === 'CRITICAL' ? 'severity-critical' :
            conflict.severity === 'WARNING' ? 'severity-warning' : 'severity-ok';
        const severityIcon = conflict.severity === 'CRITICAL' ? 'ban' :
            conflict.severity === 'WARNING' ? 'warning' : 'check';

        html += `
            <tr>
                <td><strong>${conflict.plot1Number || 'ID:' + conflict.plot1Id}</strong></td>
                <td><strong>${conflict.plot2Number || 'ID:' + conflict.plot2Id}</strong></td>
                <td>${(conflict.overlapArea || 0).toFixed(2)}</td>
                <td>
                    <span class="${severityClass}">
                        <span uk-icon="icon: ${severityIcon}"></span>
                        ${conflict.severity || 'OK'}
                    </span>
                </td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    conflictBlock.innerHTML = html;
    conflictBlock.style.display = 'block';

    if (window.UIkit) {
        UIkit.icon(conflictBlock);
    }
}

// ==========================================
// Отрисовка делян
// ==========================================
function renderPlots(plots) {
    if (!map) return;

    if (polygonLayer) {
        map.removeLayer(polygonLayer);
        polygonLayer = null;
    }
    if (labelLayer) {
        map.removeLayer(labelLayer);
        labelLayer = null;
    }

    polygonLayer = L.layerGroup().addTo(map);
    labelLayer = L.layerGroup().addTo(map);

    plots.forEach(plot => {
        if (plot.geometryGeoJson) {
            try {
                const geojson = JSON.parse(plot.geometryGeoJson);
                if (geojson.type === 'Polygon' && geojson.coordinates) {
                    const coords = geojson.coordinates[0].map(c => [c[1], c[0]]);

                    const polygon = L.polygon(coords, {
                        color: '#d32f2f',
                        weight: 2.5,
                        opacity: 0.9,
                        fillColor: '#d32f2f',
                        fillOpacity: 0.2
                    }).addTo(polygonLayer);

                    let areaHa = 'н/д';
                    if (plot.areaHa !== undefined && plot.areaHa !== null) {
                        areaHa = plot.areaHa.toFixed(2);
                    } else if (plot.areaM2) {
                        areaHa = (plot.areaM2 / 10000).toFixed(2);
                    }

                    let cutInfo = '';
                    if (plot.cutType) {
                        cutInfo += `<br><strong>Тип рубки:</strong> ${plot.cutType}`;
                    }
                    if (plot.yearOfCut) {
                        cutInfo += `<br><strong>Год рубки:</strong> ${plot.yearOfCut}`;
                    }

                    polygon.bindPopup(`
                        <div style="min-width: 220px;">
                            <b style="font-size: 16px; color: #d32f2f;">${plot.fullNumber || plot.numberInQuarter}</b><br>
                            <span style="color: #666;">${plot.forestryName || 'Без лесничества'}</span><br>
                            <span style="font-weight: bold;">${plot.verified ? '✅ Верифицирована' : '⏳ Не проверена'}</span>
                            ${cutInfo}
                            <hr style="margin: 6px 0;">
                            <small>
                                <strong>Площадь:</strong> ${areaHa} га<br>
                                <strong>Квартал:</strong> ${plot.quarterNumber || 'н/д'}
                            </small>
                        </div>
                    `);

                    if (showLabels) {
                        const center = getPolygonCenter(coords);

                        let labelText = '';
                        if (plot.quarterNumber) {
                            labelText += `Кв.${plot.quarterNumber}`;
                        }
                        if (plot.numberInQuarter) {
                            labelText += labelText ? ` / Дел.${plot.numberInQuarter}` : `Дел.${plot.numberInQuarter}`;
                        }
                        if (areaHa !== 'н/д') {
                            labelText += labelText ? ` / ${areaHa} га` : `${areaHa} га`;
                        }

                        if (!labelText) {
                            labelText = `ID:${plot.id}`;
                        }

                        const labelHtml = `
                            <div style="
                                background: rgba(255, 255, 255, 0.92);
                                color: #1a1a1a;
                                font-weight: 600;
                                font-size: 11px;
                                padding: 3px 10px;
                                border-radius: 12px;
                                border: 2px solid #d32f2f;
                                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
                                text-shadow: 0 0 4px rgba(255,255,255,0.8);
                                pointer-events: none;
                                white-space: nowrap;
                                font-family: 'Segoe UI', Arial, sans-serif;
                                transition: all 0.2s ease;
                                line-height: 1.4;
                                text-align: center;
                            ">
                                <div style="font-weight: 700; font-size: 12px; color: #d32f2f;">
                                    ${labelText}
                                </div>
                            </div>
                        `;

                        const icon = L.divIcon({
                            className: 'plot-label',
                            html: labelHtml,
                            iconSize: [0, 0],
                            iconAnchor: [0, 0]
                        });

                        const label = L.marker([center.lat, center.lng], {
                            icon: icon,
                            interactive: false,
                            keyboard: false,
                            zIndexOffset: 1000
                        }).addTo(labelLayer);

                        polygon.on('mouseover', function(e) {
                            this.setStyle({ fillOpacity: 0.4, weight: 3 });
                            const labelEl = label._icon;
                            if (labelEl) {
                                const div = labelEl.querySelector('div');
                                if (div) {
                                    div.style.transform = 'scale(1.15)';
                                    div.style.boxShadow = '0 4px 16px rgba(0,0,0,0.35)';
                                    div.style.borderColor = '#b71c1c';
                                }
                            }
                            this._container.style.cursor = 'pointer';
                        });

                        polygon.on('mouseout', function(e) {
                            this.setStyle({ fillOpacity: 0.2, weight: 2.5 });
                            const labelEl = label._icon;
                            if (labelEl) {
                                const div = labelEl.querySelector('div');
                                if (div) {
                                    div.style.transform = 'scale(1)';
                                    div.style.boxShadow = '0 2px 8px rgba(0,0,0,0.25)';
                                    div.style.borderColor = '#d32f2f';
                                }
                            }
                        });
                    }
                }
            } catch(e) {
                console.error('Ошибка при отображении деляны:', plot.fullNumber, e);
            }
        }
    });

    updateLegend(plots);
}

function formatNumberInQuarter(value) {
    if (!value) return '';

    let normalized = value.replace(/[,;]/g, ' ');
    normalized = normalized.replace(/\s+/g, ' ');
    normalized = normalized.trim();

    if (!normalized) return '';

    let numbers = normalized.split(' ').filter(s => s !== '');

    numbers.sort((a, b) => {
        const numA = parseFloat(a);
        const numB = parseFloat(b);
        if (!isNaN(numA) && !isNaN(numB)) {
            return numA - numB;
        }
        return a.localeCompare(b);
    });

    return numbers.join(', ');
}

function toggleLabels() {
    showLabels = !showLabels;

    const btnText = document.getElementById('toggleLabelsText');
    if (btnText) {
        btnText.textContent = showLabels ? 'Скрыть метки' : 'Показать метки';
    }

    const btn = document.getElementById('toggleLabelsBtn');
    if (btn) {
        if (showLabels) {
            btn.classList.remove('uk-button-danger');
            btn.classList.add('uk-button-default');
        } else {
            btn.classList.remove('uk-button-default');
            btn.classList.add('uk-button-danger');
        }
    }

    if (cachedPlots) {
        renderPlots(cachedPlots);
    } else {
        refreshMap();
    }

    UIkit.notification({
        message: showLabels ? '✅ Метки включены' : '❌ Метки скрыты',
        status: showLabels ? 'success' : 'warning',
        timeout: 1500
    });
}

// ==========================================
// ИНИЦИАЛИЗАЦИЯ
// ==========================================

document.addEventListener('DOMContentLoaded', function () {
    console.log('✅ Инициализация страницы cutting-area.html');

    if (typeof initMap === 'undefined') {
        console.error('❌ Функция initMap не найдена!');
        return;
    }

    if (typeof loadForestries === 'undefined') {
        console.error('❌ Функция loadForestries не найдена!');
        return;
    }

    loadForestries();

    if (typeof updateCoordCounter === 'function') {
        updateCoordCounter();
    }

    if (typeof updateTerritoryInfo === 'function') {
        updateTerritoryInfo();
    }

    const conflictRows = document.querySelectorAll('.conflict-list tbody tr');
    if (conflictRows.length > 0) {
        const conflictCount = document.getElementById('conflictCount');
        if (conflictCount) {
            conflictCount.textContent = conflictRows.length;
        }
    }

    const numberInQuarterInput = document.getElementById('numberInQuarter');
    if (numberInQuarterInput) {
        numberInQuarterInput.addEventListener('input', function() {
            updateTerritoryInfo();
        });
    }

    const selectors = ['forestrySelect', 'subForestrySelect', 'technicalUnitSelect'];
    selectors.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('change', function() {
                updateTerritoryInfo();
            });
        }
    });

    setTimeout(function() {
        console.log('🔄 Запускаем initMap с задержкой 300мс...');
        initMap(mapElement,getMapCenterCoordinates(),8);

        setTimeout(function() {
            if (map) {
                map.invalidateSize();
                console.log('🔄 Размер карты принудительно обновлён');
            }
        }, 500);
    }, 300);

    console.log('✅ Инициализация завершена');
});