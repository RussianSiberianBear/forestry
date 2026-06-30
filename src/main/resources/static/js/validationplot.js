// ==========================================
// ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ
// ==========================================

let showLabels = true;
let cachedPlots = null;
let polygonLayer = null;
let labelLayer = null;

// ==========================================
// ФИЛЬТРЫ ДЛЯ КАРТЫ
// ==========================================

let currentFilters = {};

function collectFilters() {
    // Используем автосборку фильтров из отдельного файла
    // Корневой элемент - форма с id=plotForm
    // Функция collectFilterAuto определена в отдельном файле
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
        // Фоллбэк - ручной сбор фильтров, если автосборка недоступна
        console.warn('⚠️ Функция collectFilterAuto не найдена, используем ручной сбор');
        return collectFiltersManual();
    }
}

// Ручной сбор фильтров (фоллбэк)
function collectFiltersManual() {
    const regionSelect = document.getElementById('regionSelect');
    const municipalSelect = document.getElementById('municipalDistrictSelect');
    const forestrySelect = document.getElementById('forestrySelect');
    const districtForestrySelect = document.getElementById('districtForestrySelect');
    const technicalUnitSelect = document.getElementById('technicalUnitSelect');
    const quarterId = document.getElementById('quarterId');
    const cutTypeSelect = document.getElementById('cutType');
    const yearOfCutInput = document.getElementById('yearOfCut');
    const numberInQuarterInput = document.getElementById('numberInQuarter');

    const filters = {};

    const rId = regionSelect?.value;
    if (rId && rId !== '') filters.regionId = rId;

    const mId = municipalSelect?.value;
    if (mId && mId !== '') filters.municipalDistrictId = mId;

    const fId = forestrySelect?.value;
    if (fId && fId !== '') filters.forestryId = fId;

    const dfId = districtForestrySelect?.value;
    if (dfId && dfId !== '') filters.districtForestryId = dfId;

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

    const mapContainer = document.getElementById('map');
    if (mapContainer) {
        mapContainer.style.opacity = '0.6';
    }

    // Формируем параметры запроса
    const params = new URLSearchParams();
    Object.keys(currentFilters).forEach(key => {
        params.append(key, currentFilters[key]);
    });

    // Определяем URL для запроса
    const hasFilters = Object.keys(currentFilters).length > 0;
    const url = hasFilters
        ? '/api/plots/map-data-filtered?' + params.toString()
        : '/api/plots/map-data';

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
                    if (currentFilters.numberInQuarter) {
                        filterText += ` (Дел. №${currentFilters.numberInQuarter})`;
                    }
                    infoSpan.textContent = filterText;
                    infoSpan.style.background = '#2e7d32';
                }
            }

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
    const regionSelect = document.getElementById('regionSelect');
    if (regionSelect) regionSelect.value = '';
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
    const territoryUnitId = document.getElementById('uiTerritoryUnitId')?.value;
    const territoryType = document.getElementById('uiTerritoryType')?.value;
    const centerLat = document.getElementById('uiCenterLat')?.value;
    const centerLng = document.getElementById('uiCenterLng')?.value;
    const zoom = document.getElementById('uiZoom')?.value;
    const cutType = document.getElementById('uiCutType')?.value;
    const yearOfCut = document.getElementById('uiYearOfCut')?.value;

    console.log('📥 Загружены настройки:', {
        territoryUnitId, territoryType, cutType, yearOfCut
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

    if (!territoryUnitId || territoryUnitId === '') {
        console.log('⚠️ Нет сохранённой территории');
        refreshMap();
        return;
    }

    loadTerritoryHierarchy(territoryUnitId, territoryType);
}

function loadTerritoryHierarchy(unitId, type) {
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
    const savedId = document.getElementById('uiTerritoryUnitId')?.value;
    if (savedId) {
        fetch('/api/territory/path/' + savedId)
            .then(response => response.json())
            .then(path => {
                let regionId = null;
                let municipalDistrictId = null;
                let forestryId = null;
                let districtForestryId = null;
                let technicalUnitId = null;
                let quarterId = null;

                path.forEach(item => {
                    switch (item.type) {
                        case 'REGION':
                            regionId = item.id;
                            break;
                        case 'MUNICIPAL_DISTRICT':
                            municipalDistrictId = item.id;
                            break;
                        case 'FORESTRY':
                            forestryId = item.id;
                            break;
                        case 'DISTRICT_FORESTRY':
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

                restoreHierarchy(regionId, municipalDistrictId, forestryId,
                    districtForestryId, technicalUnitId, quarterId);
            })
            .catch(error => {
                console.error('Ошибка загрузки пути:', error);
                refreshMap();
            });
    }
}

function restoreHierarchy(regionId, municipalDistrictId, forestryId,
                          districtForestryId, technicalUnitId, quarterId) {
    const regionSelect = document.getElementById('regionSelect');
    if (regionSelect && regionId) {
        regionSelect.value = regionId;
        loadMunicipalDistricts(regionId, function() {
            const municipalSelect = document.getElementById('municipalDistrictSelect');
            if (municipalSelect && municipalDistrictId) {
                municipalSelect.value = municipalDistrictId;
                loadForestries(municipalDistrictId, function() {
                    const forestrySelect = document.getElementById('forestrySelect');
                    if (forestrySelect && forestryId) {
                        forestrySelect.value = forestryId;
                        enableQuarterField();
                        loadDistrictForestries(forestryId, function() {
                            const districtSelect = document.getElementById('districtForestrySelect');
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
                });
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
                }
            })
            .catch(error => console.error('Ошибка загрузки квартала:', error));
    }
}

// ==========================================
// ЗАГРУЗКА СПИСКОВ (С DTO)
// ==========================================

function loadRegions(callback) {
    const regionSelect = document.getElementById('regionSelect');
    if (!regionSelect) return;

    if (regionSelect.options.length > 1) {
        console.log('✅ Регионы уже загружены');
        if (callback) callback();
        return;
    }

    console.log('📥 Загружаем список регионов...');

    fetch('/api/territory/regions')
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(regions => {
            regionSelect.innerHTML = '<option value="">-- Выберите регион --</option>';

            regions.forEach(region => {
                const option = document.createElement('option');
                option.value = region.id;
                option.textContent = region.name;
                regionSelect.appendChild(option);
            });

            console.log('✅ Загружено ' + regions.length + ' регионов');

            if (callback) callback();
        })
        .catch(error => {
            console.error('❌ Ошибка загрузки регионов:', error);
            regionSelect.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            if (callback) callback();
        });
}

function loadMunicipalDistricts(regionId, callback) {
    if (!regionId) {
        resetAllDependentSelects();
        if (callback) callback();
        return;
    }

    const select = document.getElementById('municipalDistrictSelect');
    if (!select) return;

    select.classList.add('loading');
    select.innerHTML = '<option value="">Загрузка...</option>';
    select.disabled = true;

    fetch('/api/territory/municipal-districts/by-region/' + regionId)
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(data => {
            select.innerHTML = '';
            if (Array.isArray(data) && data.length > 0) {
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = '-- Выберите район --';
                select.appendChild(defaultOption);

                data.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.textContent = item.name || 'Без названия';
                    select.appendChild(option);
                });
                select.disabled = false;

                if (data.length === 1) {
                    select.value = data[0].id;
                    saveUISetting('municipal-district', data[0].id);
                    loadForestries(data[0].id);
                }
            } else {
                select.innerHTML = '<option value="">-- Районы не найдены --</option>';
                select.disabled = true;
            }
            select.classList.remove('loading');
            updateTerritoryInfo();
            if (callback) callback();
        })
        .catch(error => {
            console.error('Ошибка загрузки районов:', error);
            select.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            select.classList.remove('loading');
            if (callback) callback();
            UIkit.notification({
                message: 'Ошибка загрузки районов',
                status: 'danger',
                timeout: 3000
            });
        });
}

function loadForestries(municipalDistrictId, callback) {
    if (!municipalDistrictId) {
        resetDependentSelects('forestry');
        if (callback) callback();
        return;
    }

    const select = document.getElementById('forestrySelect');
    if (!select) return;

    select.classList.add('loading');
    select.innerHTML = '<option value="">Загрузка...</option>';
    select.disabled = true;

    fetch('/api/territory/forestries/by-district/' + municipalDistrictId)
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(data => {
            select.innerHTML = '';
            if (Array.isArray(data) && data.length > 0) {
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = '-- Выберите лесничество --';
                select.appendChild(defaultOption);

                data.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.textContent = item.name || 'Без названия';
                    select.appendChild(option);
                });
                select.disabled = false;

                if (data.length === 1) {
                    select.value = data[0].id;
                    saveUISetting('forestry', data[0].id);
                    enableQuarterField();
                    loadDistrictForestries(data[0].id);
                }
            } else {
                select.innerHTML = '<option value="">-- Лесничества не найдены --</option>';
                select.disabled = true;
            }
            select.classList.remove('loading');
            updateTerritoryInfo();
            if (callback) callback();
        })
        .catch(error => {
            console.error('Ошибка загрузки лесничеств:', error);
            select.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            select.classList.remove('loading');
            if (callback) callback();
        });
}

function loadDistrictForestries(forestryId, callback) {
    if (!forestryId) {
        resetDependentSelects('district');
        if (callback) callback();
        return;
    }

    const select = document.getElementById('districtForestrySelect');
    if (!select) return;

    select.classList.add('loading');
    select.innerHTML = '<option value="">Загрузка...</option>';
    select.disabled = true;

    fetch('/api/territory/district-forestries/by-forestry/' + forestryId)
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(data => {
            select.innerHTML = '';
            if (Array.isArray(data) && data.length > 0) {
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = '-- Выберите участковое лесничество --';
                select.appendChild(defaultOption);

                data.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.textContent = item.name || 'Без названия';
                    select.appendChild(option);
                });
                select.disabled = false;

                if (data.length === 1) {
                    select.value = data[0].id;
                    saveUISetting('district-forestry', data[0].id);
                    loadTechnicalUnits(data[0].id);
                }
            } else {
                select.innerHTML = '<option value="">-- Участковые лесничества не найдены --</option>';
                select.disabled = true;
            }
            select.classList.remove('loading');
            updateTerritoryInfo();
            if (callback) callback();
        })
        .catch(error => {
            console.error('Ошибка загрузки участковых лесничеств:', error);
            select.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            select.classList.remove('loading');
            if (callback) callback();
        });
}

function loadTechnicalUnits(districtForestryId, callback) {
    console.log('🔍 loadTechnicalUnits вызван с districtForestryId:', districtForestryId);

    const techSelect = document.getElementById('technicalUnitSelect');
    if (!techSelect) {
        console.error('❌ technicalUnitSelect не найден');
        return;
    }

    if (!districtForestryId) {
        techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
        techSelect.disabled = true;
        if (callback) callback();
        return;
    }

    techSelect.classList.add('loading');
    techSelect.innerHTML = '<option value="">Загрузка...</option>';
    techSelect.disabled = true;

    fetch('/api/territory/technical-units/by-district/' + districtForestryId)
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(data => {
            console.log('📥 Получены технические участки:', data);
            techSelect.innerHTML = '';
            if (Array.isArray(data) && data.length > 0) {
                if (data.length === 1) {
                    const option = document.createElement('option');
                    option.value = data[0].id;
                    option.textContent = data[0].name || 'Без названия';
                    option.selected = true;
                    techSelect.appendChild(option);
                    techSelect.disabled = false;
                    saveUISetting('technical-unit', data[0].id);
                } else {
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = '-- Выберите технический участок --';
                    techSelect.appendChild(defaultOption);

                    data.forEach(item => {
                        const option = document.createElement('option');
                        option.value = item.id;
                        option.textContent = item.name || 'Без названия';
                        techSelect.appendChild(option);
                    });
                    techSelect.disabled = false;
                }
            } else {
                techSelect.innerHTML = '<option value="">-- Технические участки не найдены --</option>';
                techSelect.disabled = true;
            }
            techSelect.classList.remove('loading');
            updateTerritoryInfo();
            if (callback) callback();
        })
        .catch(error => {
            console.error('❌ Ошибка загрузки технических участков:', error);
            techSelect.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            techSelect.classList.remove('loading');
            techSelect.disabled = true;
            if (callback) callback();
        });
}

// ==========================================
// КВАРТАЛЫ (AUTOCOMPLETE)
// ==========================================

let quarterSearchTimeout = null;

function searchQuarters(query) {
    const technicalUnitId = document.getElementById('technicalUnitSelect')?.value;
    const districtForestryId = document.getElementById('districtForestrySelect')?.value;
    const forestryId = document.getElementById('forestrySelect')?.value;
    const suggestionsDiv = document.getElementById('quarterSuggestions');
    const quarterInput = document.getElementById('quarterInput');

    if (!quarterInput) return;

    let parentId = technicalUnitId || districtForestryId || forestryId;

    if (!parentId) {
        if (suggestionsDiv) {
            suggestionsDiv.innerHTML = '<div class="no-results">Сначала выберите лесничество</div>';
            suggestionsDiv.style.display = 'block';
        }
        return;
    }

    if (!query || query.trim().length === 0) {
        if (suggestionsDiv) suggestionsDiv.style.display = 'none';
        return;
    }

    clearTimeout(quarterSearchTimeout);
    quarterSearchTimeout = setTimeout(() => {
        if (suggestionsDiv) {
            suggestionsDiv.innerHTML = '<div class="loading-suggestions">Поиск...</div>';
            suggestionsDiv.style.display = 'block';
        }

        const url = '/api/territory/quarters/search?technicalUnitId=' + parentId + '&query=' + encodeURIComponent(query.trim());

        fetch(url)
            .then(response => {
                if (!response.ok) throw new Error('HTTP ' + response.status);
                return response.json();
            })
            .then(data => {
                if (!suggestionsDiv) return;

                if (!Array.isArray(data) || data.length === 0) {
                    suggestionsDiv.innerHTML = '<div class="no-results">Кварталы не найдены</div>';
                    return;
                }

                let html = '';
                data.forEach(item => {
                    html += `
                        <div class="suggestion-item"
                             data-id="${item.id}"
                             data-number="${item.number}"
                             onclick="selectQuarter(${item.id}, '${item.number}', '${item.name || ''}')">
                            <span class="suggestion-number">Кв. ${item.number}</span>
                            ${item.name ? `<span class="suggestion-name">${item.name}</span>` : ''}
                            ${item.areaHa ? `<span class="suggestion-name">${item.areaHa.toFixed(1)} га</span>` : ''}
                        </div>
                    `;
                });
                suggestionsDiv.innerHTML = html;
                suggestionsDiv.style.display = 'block';
            })
            .catch(error => {
                console.error('Ошибка поиска кварталов:', error);
                if (suggestionsDiv) {
                    suggestionsDiv.innerHTML = '<div class="no-results">❌ Ошибка загрузки</div>';
                }
            });
    }, 300);
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ==========================================

function resetDependentSelects(level) {
    if (!level || level === 'forestry') {
        const districtSelect = document.getElementById('districtForestrySelect');
        if (districtSelect) {
            districtSelect.innerHTML = '<option value="">-- Сначала выберите лесничество --</option>';
            districtSelect.disabled = true;
        }
    }

    if (!level || level === 'forestry' || level === 'district') {
        const techSelect = document.getElementById('technicalUnitSelect');
        if (techSelect) {
            techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
            techSelect.disabled = true;
        }
    }

    const quarterInput = document.getElementById('quarterInput');
    if (quarterInput) {
        quarterInput.disabled = true;
        quarterInput.value = '';
        quarterInput.placeholder = 'Сначала выберите лесничество';
    }
    const quarterId = document.getElementById('quarterId');
    if (quarterId) quarterId.value = '';
    const suggestions = document.getElementById('quarterSuggestions');
    if (suggestions) suggestions.style.display = 'none';
}

function resetAllDependentSelects() {
    const districtSelect = document.getElementById('municipalDistrictSelect');
    if (districtSelect) {
        districtSelect.innerHTML = '<option value="">-- Сначала выберите регион --</option>';
        districtSelect.disabled = true;
        districtSelect.classList.remove('loading');
    }

    const forestrySelect = document.getElementById('forestrySelect');
    if (forestrySelect) {
        forestrySelect.innerHTML = '<option value="">-- Сначала выберите район --</option>';
        forestrySelect.disabled = true;
        forestrySelect.classList.remove('loading');
    }

    const districtForestrySelect = document.getElementById('districtForestrySelect');
    if (districtForestrySelect) {
        districtForestrySelect.innerHTML = '<option value="">-- Сначала выберите лесничество --</option>';
        districtForestrySelect.disabled = true;
        districtForestrySelect.classList.remove('loading');
    }

    const techSelect = document.getElementById('technicalUnitSelect');
    if (techSelect) {
        techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
        techSelect.disabled = true;
        techSelect.classList.remove('loading');
    }

    const quarterInput = document.getElementById('quarterInput');
    if (quarterInput) {
        quarterInput.disabled = true;
        quarterInput.value = '';
        quarterInput.placeholder = 'Сначала выберите лесничество';
    }
    const quarterId = document.getElementById('quarterId');
    if (quarterId) quarterId.value = '';
    const suggestions = document.getElementById('quarterSuggestions');
    if (suggestions) suggestions.style.display = 'none';

    updateTerritoryInfo();
}

document.addEventListener('click', function(e) {
    const container = document.getElementById('quarterInput')?.closest('.autocomplete-wrapper');
    if (container && !container.contains(e.target)) {
        const suggestions = document.getElementById('quarterSuggestions');
        if (suggestions) suggestions.style.display = 'none';
    }
});

// ==========================================
// ОБРАБОТЧИКИ ИЗМЕНЕНИЙ
// ==========================================

function onRegionChange(regionId) {
    resetAllDependentSelects();

    if (!regionId) {
        saveUISetting('territory-unit', 0);
        saveUISetting('territory-type', '');
        return;
    }

    saveUISetting('territory-unit', regionId);
    saveUISetting('territory-type', 'REGION');
    loadMunicipalDistricts(regionId);

    updateTerritoryInfo();
}

function onMunicipalDistrictChange(municipalDistrictId) {
    resetDependentSelects('forestry');

    if (!municipalDistrictId) {
        saveUISetting('territory-unit', 0);
        saveUISetting('territory-type', '');
        return;
    }

    saveUISetting('territory-unit', municipalDistrictId);
    saveUISetting('territory-type', 'MUNICIPAL_DISTRICT');
    loadForestries(municipalDistrictId);

    updateTerritoryInfo();
}

function onForestryChange(forestryId) {
    resetDependentSelects('district');

    if (!forestryId) {
        saveUISetting('territory-unit', 0);
        saveUISetting('territory-type', '');
        const quarterInput = document.getElementById('quarterInput');
        if (quarterInput) {
            quarterInput.disabled = true;
            quarterInput.value = '';
            quarterInput.placeholder = 'Сначала выберите лесничество';
        }
        return;
    }

    const quarterInput = document.getElementById('quarterInput');
    if (quarterInput) {
        quarterInput.disabled = false;
        quarterInput.placeholder = 'Введите номер квартала...';
    }

    saveUISetting('territory-unit', forestryId);
    saveUISetting('territory-type', 'FORESTRY');
    loadDistrictForestries(forestryId);

    updateTerritoryInfo();
}

function onDistrictForestryChange(districtForestryId) {
    console.log('🔄 onDistrictForestryChange вызван с districtForestryId:', districtForestryId);

    const techSelect = document.getElementById('technicalUnitSelect');
    const quarterInput = document.getElementById('quarterInput');

    if (!districtForestryId) {
        if (techSelect) {
            techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
            techSelect.disabled = true;
        }
        saveUISetting('territory-unit', 0);
        saveUISetting('territory-type', '');
        return;
    }

    if (techSelect) {
        techSelect.innerHTML = '<option value="">Загрузка...</option>';
        techSelect.disabled = true;
    }

    saveUISetting('territory-unit', districtForestryId);
    saveUISetting('territory-type', 'DISTRICT_FORESTRY');

    loadTechnicalUnits(districtForestryId);

    updateTerritoryInfo();
}

function onTechnicalUnitChange(technicalUnitId) {
    const quarterInput = document.getElementById('quarterInput');

    if (!technicalUnitId) {
        saveUISetting('territory-unit', 0);
        saveUISetting('territory-type', '');
        return;
    }

    saveUISetting('territory-unit', technicalUnitId);
    saveUISetting('territory-type', 'TECHNICAL_UNIT');

    updateTerritoryInfo();
}

function selectQuarter(id, number, name) {
    const quarterInput = document.getElementById('quarterInput');
    const quarterIdInput = document.getElementById('quarterId');
    const suggestionsDiv = document.getElementById('quarterSuggestions');

    if (quarterInput) {
        quarterInput.value = 'Кв. ' + number + (name ? ' (' + name + ')' : '');
        quarterInput.disabled = false;
    }
    if (quarterIdInput) quarterIdInput.value = id;
    if (suggestionsDiv) suggestionsDiv.style.display = 'none';

    saveUISetting('territory-unit', id);
    saveUISetting('territory-type', 'QUARTER');
    updateTerritoryInfo();

    UIkit.notification({
        message: '✅ Выбран квартал ' + number,
        status: 'success',
        timeout: 1500
    });
}

function onCutTypeChange(value) {
    console.log('🔄 Изменён тип рубки:', value);
    saveUISetting('cut-type', value || '');
}

function onYearOfCutChange(value) {
    console.log('🔄 Изменён год рубки:', value);
    saveUISetting('year-of-cut', value || '');
}

// ==========================================
// ОБНОВЛЕНИЕ ИНФОРМАЦИИ О ТЕРРИТОРИИ
// ==========================================

function updateTerritoryInfo() {
    const regionId = document.getElementById('regionSelect')?.value;
    const municipalDistrictId = document.getElementById('municipalDistrictSelect')?.value;
    const forestryId = document.getElementById('forestrySelect')?.value;
    const districtForestryId = document.getElementById('districtForestrySelect')?.value;
    const technicalUnitId = document.getElementById('technicalUnitSelect')?.value;
    const quarterId = document.getElementById('quarterId')?.value;
    const quarterInput = document.getElementById('quarterInput')?.value;
    const numberInQuarter = document.getElementById('numberInQuarter')?.value?.trim();

    let name = 'не выбрано';

    if (quarterId && quarterId !== '') {
        name = quarterInput || 'Квартал';
    } else if (technicalUnitId && technicalUnitId !== '') {
        const select = document.getElementById('technicalUnitSelect');
        name = select?.options[select.selectedIndex]?.text || 'Технический участок';
    } else if (districtForestryId && districtForestryId !== '') {
        const select = document.getElementById('districtForestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Участковое лесничество';
    } else if (forestryId && forestryId !== '') {
        const select = document.getElementById('forestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Лесничество';
    } else if (municipalDistrictId && municipalDistrictId !== '') {
        const select = document.getElementById('municipalDistrictSelect');
        name = select?.options[select.selectedIndex]?.text || 'Муниципальный район';
    } else if (regionId && regionId !== '') {
        const select = document.getElementById('regionSelect');
        name = select?.options[select.selectedIndex]?.text || 'Регион';
    }

    if (numberInQuarter) {
        name += ` (Дел. №${numberInQuarter})`;
    }

    const span = document.getElementById('selectedTerritoryName');
    if (span) span.textContent = name;
}

// ==========================================
// ПРОВЕРКА ВЫБРАННОЙ ТЕРРИТОРИИ
// ==========================================

function checkSelected() {
    const regionId = document.getElementById('regionSelect')?.value;
    const municipalDistrictId = document.getElementById('municipalDistrictSelect')?.value;
    const forestryId = document.getElementById('forestrySelect')?.value;
    const districtForestryId = document.getElementById('districtForestrySelect')?.value;
    const technicalUnitId = document.getElementById('technicalUnitSelect')?.value;
    const quarterId = document.getElementById('quarterId')?.value;
    const numberInQuarter = document.getElementById('numberInQuarter')?.value?.trim();

    let type = null;
    let id = null;
    let name = '';

    if (quarterId && quarterId !== '') {
        type = 'QUARTER';
        id = quarterId;
        name = document.getElementById('quarterInput')?.value || 'Квартал';
    } else if (technicalUnitId && technicalUnitId !== '') {
        type = 'TECHNICAL_UNIT';
        id = technicalUnitId;
        const select = document.getElementById('technicalUnitSelect');
        name = select?.options[select.selectedIndex]?.text || 'Технический участок';
    } else if (districtForestryId && districtForestryId !== '') {
        type = 'DISTRICT_FORESTRY';
        id = districtForestryId;
        const select = document.getElementById('districtForestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Участковое лесничество';
    } else if (forestryId && forestryId !== '') {
        type = 'FORESTRY';
        id = forestryId;
        const select = document.getElementById('forestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Лесничество';
    } else if (municipalDistrictId && municipalDistrictId !== '') {
        type = 'MUNICIPAL_DISTRICT';
        id = municipalDistrictId;
        const select = document.getElementById('municipalDistrictSelect');
        name = select?.options[select.selectedIndex]?.text || 'Муниципальный район';
    } else if (regionId && regionId !== '') {
        type = 'REGION';
        id = regionId;
        const select = document.getElementById('regionSelect');
        name = select?.options[select.selectedIndex]?.text || 'Регион';
    } else {
        UIkit.notification({
            message: '❌ Выберите территорию в форме выше (регион, район, лесничество или участковое)',
            status: 'warning',
            timeout: 4000
        });
        return;
    }

    const typeNames = {
        'REGION': 'региону',
        'MUNICIPAL_DISTRICT': 'муниципальному району',
        'FORESTRY': 'лесничеству',
        'DISTRICT_FORESTRY': 'участковому лесничеству',
        'TECHNICAL_UNIT': 'техническому участку',
        'QUARTER': 'кварталу'
    };

    let message = `🔍 Проверка делян по ${typeNames[type]}: "${name}"...`;
    if (numberInQuarter) {
        message += ` (Дел. №${numberInQuarter})`;
    }

    UIkit.notification({
        message: message,
        status: 'primary',
        timeout: 3000
    });

    const params = new URLSearchParams();
    params.append('type', type);
    params.append('id', id);
    if (numberInQuarter) {
        params.append('numberInQuarter', numberInQuarter);
    }

    fetch('/api/plots/validate-by-territory?' + params.toString(), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(text || 'HTTP ' + response.status);
                });
            }
            return response.json();
        })
        .then(conflicts => {
            UIkit.notification.closeAll();

            if (conflicts.length === 0) {
                UIkit.notification({
                    message: `✅ Все деляны по "${name}" проверены! Пересечений не обнаружено.`,
                    status: 'success',
                    timeout: 5000
                });

                const conflictBlock = document.getElementById('conflictResults');
                if (conflictBlock) {
                    conflictBlock.style.display = 'none';
                }
            } else {
                UIkit.notification({
                    message: `⚠️ Найдено ${conflicts.length} пересечений по "${name}"! Проверьте список ниже.`,
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
// ПРОВЕРКА ВСЕХ ДЕЛЯН
// ==========================================

function checkIntersectAll() {
    UIkit.notification({
        message: '🔍 Запуск глобальной проверки всех делян...',
        status: 'primary',
        timeout: 3000
    });

    fetch('/api/plots/validate-all', {
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
// КАРТА
// ==========================================

let map = null;
let osmLayer = null;
let googleSatLayer = null;

function initMap() {
    try {
        if (document.getElementById('map')) {
            map = L.map('map').setView([56.0, 92.0], 6);

            osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
                maxZoom: 19
            });

            googleSatLayer = L.tileLayer('https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
                maxZoom: 20,
                subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
                attribution: '© Google Maps'
            });

            var baseMaps = {
                "🗺️ Схема": osmLayer,
                "🛰️ Спутник": googleSatLayer
            };

            googleSatLayer.addTo(map);
            L.control.layers(baseMaps).addTo(map);

            loadUISettingsFromServer();

            const regionSelect = document.getElementById('regionSelect');
            if (regionSelect) {
                const options = regionSelect.querySelectorAll('option');
                const regionOptions = Array.from(options).filter(opt => opt.value !== '');
                if (regionOptions.length === 1 && !regionSelect.value) {
                    const regionId = regionOptions[0].value;
                    regionSelect.value = regionId;
                    onRegionChange(regionId);
                }
            }

            updateCoordCounter();
            updateTerritoryInfo();

            console.log('✅ Карта инициализирована');
        } else {
            console.warn('⚠️ Элемент #map не найден на странице');
        }
    } catch (e) {
        console.error('❌ Ошибка инициализации карты:', e);
    }
}

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

    updateLegend();
}

function getPolygonCenter(coords) {
    let lat = 0, lng = 0;
    coords.forEach(c => {
        lat += c[0];
        lng += c[1];
    });
    return {
        lat: lat / coords.length,
        lng: lng / coords.length
    };
}

function updateLegend() {
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
    if (filters.numberInQuarter) {
        filterInfo += `<div style="font-size: 10px; color: #1e87f0;">Дел. №: ${filters.numberInQuarter}</div>`;
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

    const mapContainer = document.getElementById('map');
    if (mapContainer) {
        mapContainer.style.position = 'relative';
        mapContainer.appendChild(legend);
    }
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
    console.log('✅ Инициализация страницы forest-ploat.html');

    if (typeof initMap === 'undefined') {
        console.error('❌ Функция initMap не найдена!');
        return;
    }

    if (typeof loadRegions === 'undefined') {
        console.error('❌ Функция loadRegions не найдена!');
        return;
    }

    loadRegions();

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

    const selectors = ['regionSelect', 'municipalDistrictSelect', 'forestrySelect', 'districtForestrySelect', 'technicalUnitSelect'];
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
        initMap();

        setTimeout(function() {
            if (map) {
                map.invalidateSize();
                console.log('🔄 Размер карты принудительно обновлён');
            }
        }, 500);
    }, 300);

    console.log('✅ Инициализация завершена');
});