// ==========================================
// ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ
// ==========================================

let showLabels = true;  // По умолчанию метки включены
let cachedPlots = null;
let polygonLayer = null;
let labelLayer = null;

// ==========================================
// ФИЛЬТРЫ ДЛЯ КАРТЫ (берутся из левой панели)
// ==========================================

let currentFilters = {};

function collectFilters() {
    const regionSelect = document.getElementById('regionSelect');
    const municipalSelect = document.getElementById('municipalDistrictSelect');
    const forestrySelect = document.getElementById('forestrySelect');
    const districtForestrySelect = document.getElementById('districtForestrySelect');
    const technicalUnitSelect = document.getElementById('technicalUnitSelect');
    const quarterId = document.getElementById('quarterId');
    const cutTypeSelect = document.getElementById('cutType');
    const yearOfCutInput = document.getElementById('yearOfCut');

    const filters = {};

    // Собираем ВСЕ выбранные значения
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

    console.log('📋 Собранные фильтры:', filters);
    return filters;
}

// ==========================================
// ОБНОВЛЕНИЕ КАРТЫ ПО КНОПКЕ "Обновить карту"
// ==========================================

function refreshMap() {
    // Собираем фильтры из левой панели
    currentFilters = collectFilters();
    console.log('🔍 Обновление карты с фильтрами:', currentFilters);

    // Если фильтров нет - грузим все деляны
    if (Object.keys(currentFilters).length === 0) {
        loadAllPlots();
        return;
    }

    // Строим URL с параметрами
    const params = new URLSearchParams();
    Object.keys(currentFilters).forEach(key => {
        params.append(key, currentFilters[key]);
    });

    const url = '/api/plots/map-data-filtered?' + params.toString();
    console.log('📡 Запрос к:', url);

    // Показываем индикатор загрузки на карте
    const mapContainer = document.getElementById('map');
    if (mapContainer) {
        mapContainer.style.opacity = '0.6';
    }

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

            // Обновляем информацию о количестве
            const infoSpan = document.getElementById('filterInfo');
            if (infoSpan) {
                if (Object.keys(currentFilters).length === 0) {
                    infoSpan.textContent = 'Все деляны';
                    infoSpan.style.background = '#1e87f0';
                } else {
                    infoSpan.textContent = `Найдено: ${count}`;
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
    console.log('📡 Загрузка всех делян');

    const mapContainer = document.getElementById('map');
    if (mapContainer) {
        mapContainer.style.opacity = '0.6';
    }

    const infoSpan = document.getElementById('filterInfo');
    if (infoSpan) {
        infoSpan.textContent = 'Все деляны';
        infoSpan.style.background = '#1e87f0';
    }

    fetch('/api/plots/map-data')
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(plots => {
            cachedPlots = plots;
            renderPlots(plots);

            if (mapContainer) {
                mapContainer.style.opacity = '1';
            }

            const count = plots ? plots.length : 0;
            UIkit.notification({
                message: `🗺️ Загружено ${count} делян`,
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
// КООРДИНАТЫ - ДОБАВЛЕНИЕ НОВЫХ СТРОК ПО ENTER
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
// НАСТРОЙКИ UI (СОХРАНЕНИЕ)
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
// ЗАГРУЗКА НАСТРОЕК ИЗ СКРЫТЫХ ПОЛЕЙ
// ==========================================

function loadUISettingsFromServer() {
    const regionId = document.getElementById('uiRegionId')?.value;
    const municipalDistrictId = document.getElementById('uiMunicipalDistrictId')?.value;
    const forestryId = document.getElementById('uiForestryId')?.value;
    const districtForestryId = document.getElementById('uiDistrictForestryId')?.value;
    const technicalUnitId = document.getElementById('uiTechnicalUnitId')?.value;
    const quarterId = document.getElementById('uiQuarterId')?.value;
    const centerLat = document.getElementById('uiCenterLat')?.value;
    const centerLng = document.getElementById('uiCenterLng')?.value;
    const zoom = document.getElementById('uiZoom')?.value;

    console.log('📥 Загружены настройки:', {
        regionId, municipalDistrictId, forestryId,
        districtForestryId, technicalUnitId, quarterId
    });

    if (!regionId || regionId === '') {
        console.log('⚠️ Нет сохранённого региона');
        loadAllPlots();
        return;
    }

    const regionSelect = document.getElementById('regionSelect');
    if (regionSelect) regionSelect.value = regionId;

    if (centerLat && centerLng && map) {
        map.setView([parseFloat(centerLat), parseFloat(centerLng)], parseInt(zoom) || 7);
    }

    loadMunicipalDistricts(regionId, function() {
        if (municipalDistrictId && municipalDistrictId !== '') {
            const districtSelect = document.getElementById('municipalDistrictSelect');
            if (districtSelect) districtSelect.value = municipalDistrictId;
            loadForestries(municipalDistrictId, function() {
                if (forestryId && forestryId !== '') {
                    const forestrySelect = document.getElementById('forestrySelect');
                    if (forestrySelect) forestrySelect.value = forestryId;
                    loadDistrictForestries(forestryId, function() {
                        if (districtForestryId && districtForestryId !== '') {
                            const districtSelect2 = document.getElementById('districtForestrySelect');
                            if (districtSelect2) districtSelect2.value = districtForestryId;
                            loadTechnicalUnits(districtForestryId, function() {
                                if (technicalUnitId && technicalUnitId !== '') {
                                    const techSelect = document.getElementById('technicalUnitSelect');
                                    if (techSelect) techSelect.value = technicalUnitId;
                                    loadQuarters(technicalUnitId, function() {
                                        if (quarterId && quarterId !== '') {
                                            fetch('/api/quarters/' + quarterId)
                                                .then(response => response.json())
                                                .then(quarter => {
                                                    if (quarter && quarter.number) {
                                                        const input = document.getElementById('quarterInput');
                                                        if (input) {
                                                            input.value = 'Кв. ' + quarter.number + (quarter.name ? ' (' + quarter.name + ')' : '');
                                                        }
                                                        document.getElementById('quarterId').value = quarterId;
                                                        console.log('✅ Установлен квартал:', quarter.number);
                                                        // Загружаем карту с фильтрами после восстановления настроек
                                                        refreshMap();
                                                    }
                                                })
                                                .catch(error => console.error('Ошибка загрузки квартала:', error));
                                        } else {
                                            refreshMap();
                                        }
                                    });
                                } else {
                                    loadQuarters(districtForestryId);
                                    refreshMap();
                                }
                            });
                        } else {
                            loadTechnicalUnits(districtForestryId);
                            refreshMap();
                        }
                    });
                } else {
                    loadDistrictForestries(forestryId);
                    refreshMap();
                }
            });
        } else {
            loadForestries(municipalDistrictId);
            refreshMap();
        }
    });
}

// ==========================================
// ОБРАБОТЧИКИ ИЗМЕНЕНИЙ (ТОЛЬКО СОХРАНЯЮТ НАСТРОЙКИ, НЕ ОБНОВЛЯЮТ КАРТУ!)
// ==========================================

function onRegionChange(regionId) {
    resetAllDependentSelects();

    if (!regionId) {
        saveUISetting('region', 0);
        return;
    }

    saveUISetting('region', regionId);
    loadMunicipalDistricts(regionId);
    saveUISetting('municipal-district', 0);
    saveUISetting('forestry', 0);
    saveUISetting('district-forestry', 0);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);

    updateTerritoryInfo();
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

function onMunicipalDistrictChange(municipalDistrictId) {
    resetDependentSelects('forestry');

    if (!municipalDistrictId) {
        saveUISetting('municipal-district', 0);
        return;
    }

    saveUISetting('municipal-district', municipalDistrictId);
    loadForestries(municipalDistrictId);
    saveUISetting('forestry', 0);
    saveUISetting('district-forestry', 0);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);

    updateTerritoryInfo();
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

function onForestryChange(forestryId) {
    resetDependentSelects('district');

    if (!forestryId) {
        saveUISetting('forestry', 0);
        return;
    }

    saveUISetting('forestry', forestryId);
    loadDistrictForestries(forestryId);
    saveUISetting('district-forestry', 0);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);

    updateTerritoryInfo();
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

function onDistrictForestryChange(districtForestryId) {
    console.log('🔄 onDistrictForestryChange вызван с districtForestryId:', districtForestryId);

    if (!districtForestryId) {
        const techSelect = document.getElementById('technicalUnitSelect');
        if (techSelect) {
            techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
            techSelect.disabled = true;
        }
        document.getElementById('quarterInput').disabled = true;
        document.getElementById('quarterInput').value = '';
        document.getElementById('quarterId').value = '';
        document.getElementById('quarterSuggestions').style.display = 'none';
        saveUISetting('district-forestry', 0);
        return;
    }

    const techSelect = document.getElementById('technicalUnitSelect');
    if (techSelect) {
        techSelect.innerHTML = '<option value="">Загрузка...</option>';
        techSelect.disabled = true;
    }
    document.getElementById('quarterInput').disabled = true;
    document.getElementById('quarterInput').value = '';
    document.getElementById('quarterId').value = '';
    document.getElementById('quarterSuggestions').style.display = 'none';

    saveUISetting('district-forestry', districtForestryId);
    loadTechnicalUnits(districtForestryId);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

function onTechnicalUnitChange(technicalUnitId) {
    const quarterInput = document.getElementById('quarterInput');
    if (quarterInput) {
        quarterInput.disabled = true;
        quarterInput.value = '';
    }
    document.getElementById('quarterId').value = '';
    document.getElementById('quarterSuggestions').style.display = 'none';

    if (!technicalUnitId) {
        saveUISetting('technical-unit', 0);
        return;
    }

    saveUISetting('technical-unit', technicalUnitId);
    loadQuarters(technicalUnitId);
    saveUISetting('quarter', 0);

    updateTerritoryInfo();
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

function selectQuarter(id, number, name) {
    const quarterInput = document.getElementById('quarterInput');
    const quarterIdInput = document.getElementById('quarterId');
    const suggestionsDiv = document.getElementById('quarterSuggestions');

    if (quarterInput) {
        quarterInput.value = 'Кв. ' + number + (name ? ' (' + name + ')' : '');
    }
    if (quarterIdInput) quarterIdInput.value = id;
    if (suggestionsDiv) suggestionsDiv.style.display = 'none';

    saveUISetting('quarter', id);
    updateTerritoryInfo();

    UIkit.notification({
        message: '✅ Выбран квартал ' + number,
        status: 'success',
        timeout: 1500
    });
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

// ==========================================
// ОБРАБОТЧИКИ ДЛЯ ГОДА РУБКИ И ТИПА РУБКИ
// ==========================================

function onCutTypeChange(value) {
    console.log('🔄 Изменён тип рубки:', value);
    saveUISetting('cutType', value || 0);
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

function onYearOfCutChange(value) {
    console.log('🔄 Изменён год рубки:', value);
    saveUISetting('yearOfCut', value || 0);
    // НЕ ОБНОВЛЯЕМ КАРТУ!
}

// ==========================================
// СБРОС ВСЕХ ЗАВИСИМЫХ СЕЛЕКТОВ
// ==========================================

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
    }
    const quarterId = document.getElementById('quarterId');
    if (quarterId) quarterId.value = '';
    const suggestions = document.getElementById('quarterSuggestions');
    if (suggestions) suggestions.style.display = 'none';

    updateTerritoryInfo();
}

// ==========================================
// ЗАГРУЗКА ЗАВИСИМЫХ СПИСКОВ (С CALLBACK)
// ==========================================

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

    fetch('/api/municipal-districts/by-region/' + regionId)
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

    fetch('/api/forestries/by-district/' + municipalDistrictId)
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

    fetch('/api/district-forestries/by-forestry/' + forestryId)
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

// ==========================================
// ЗАГРУЗКА ТЕХНИЧЕСКИХ УЧАСТКОВ
// ==========================================

function loadTechnicalUnits(districtForestryId, callback) {
    console.log('🔍 loadTechnicalUnits вызван с districtForestryId:', districtForestryId);

    if (!districtForestryId) {
        const techSelect = document.getElementById('technicalUnitSelect');
        if (techSelect) {
            techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
            techSelect.disabled = true;
        }
        document.getElementById('quarterInput').disabled = true;
        document.getElementById('quarterInput').value = '';
        document.getElementById('quarterId').value = '';
        document.getElementById('quarterSuggestions').style.display = 'none';
        if (callback) callback();
        return;
    }

    const select = document.getElementById('technicalUnitSelect');
    if (!select) {
        console.error('❌ technicalUnitSelect не найден');
        return;
    }

    select.classList.add('loading');
    select.innerHTML = '<option value="">Загрузка...</option>';
    select.disabled = true;

    fetch('/api/technical-units/by-district/' + districtForestryId)
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(data => {
            console.log('📥 Получены технические участки:', data);
            select.innerHTML = '';
            if (Array.isArray(data) && data.length > 0) {
                if (data.length === 1) {
                    const option = document.createElement('option');
                    option.value = data[0].id;
                    option.textContent = data[0].name || 'Без названия';
                    option.selected = true;
                    select.appendChild(option);
                    select.disabled = false;
                    saveUISetting('technical-unit', data[0].id);
                    loadQuarters(data[0].id);
                } else {
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = '-- Выберите технический участок --';
                    select.appendChild(defaultOption);

                    data.forEach(item => {
                        const option = document.createElement('option');
                        option.value = item.id;
                        option.textContent = item.name || 'Без названия';
                        select.appendChild(option);
                    });
                    select.disabled = false;
                }
            } else {
                select.innerHTML = '<option value="">-- Технические участки не найдены --</option>';
                select.disabled = true;
            }
            select.classList.remove('loading');
            updateTerritoryInfo();
            if (callback) callback();
        })
        .catch(error => {
            console.error('❌ Ошибка загрузки технических участков:', error);
            select.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            select.classList.remove('loading');
            select.disabled = true;
            if (callback) callback();
        });
}

function loadQuarters(technicalUnitId, callback) {
    const quarterInput = document.getElementById('quarterInput');
    const quarterId = document.getElementById('quarterId');
    const suggestions = document.getElementById('quarterSuggestions');

    if (!technicalUnitId) {
        if (quarterInput) {
            quarterInput.disabled = true;
            quarterInput.value = '';
        }
        if (quarterId) quarterId.value = '';
        if (suggestions) suggestions.style.display = 'none';
        if (callback) callback();
        return;
    }

    if (quarterInput) {
        quarterInput.disabled = false;
        quarterInput.placeholder = 'Введите номер квартала...';
    }
    if (callback) callback();
}

// ==========================================
// КВАРТАЛЫ (AUTOCOMPLETE)
// ==========================================

let quarterSearchTimeout = null;

function searchQuarters(query) {
    const technicalUnitId = document.getElementById('technicalUnitSelect')?.value;
    const suggestionsDiv = document.getElementById('quarterSuggestions');
    const quarterInput = document.getElementById('quarterInput');

    if (!technicalUnitId) {
        if (suggestionsDiv) {
            suggestionsDiv.innerHTML = '<div class="no-results">Сначала выберите технический участок</div>';
            suggestionsDiv.style.display = 'block';
        }
        if (quarterInput) quarterInput.disabled = true;
        return;
    }

    if (quarterInput) quarterInput.disabled = false;

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

        fetch('/api/quarters/search?technicalUnitId=' + technicalUnitId + '&query=' + encodeURIComponent(query.trim()))
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
    }
    const quarterId = document.getElementById('quarterId');
    if (quarterId) quarterId.value = '';
    const suggestions = document.getElementById('quarterSuggestions');
    if (suggestions) suggestions.style.display = 'none';
}

// Закрываем подсказки при клике вне
document.addEventListener('click', function(e) {
    const container = document.getElementById('quarterInput')?.closest('.autocomplete-wrapper');
    if (container && !container.contains(e.target)) {
        const suggestions = document.getElementById('quarterSuggestions');
        if (suggestions) suggestions.style.display = 'none';
    }
});

// ==========================================
// ОБНОВЛЕНИЕ ИНФОРМАЦИИ О ВЫБРАННОЙ ТЕРРИТОРИИ
// ==========================================

function updateTerritoryInfo() {
    const regionId = document.getElementById('regionSelect')?.value;
    const municipalDistrictId = document.getElementById('municipalDistrictSelect')?.value;
    const forestryId = document.getElementById('forestrySelect')?.value;
    const districtForestryId = document.getElementById('districtForestrySelect')?.value;
    const technicalUnitId = document.getElementById('technicalUnitSelect')?.value;
    const quarterId = document.getElementById('quarterId')?.value;
    const quarterInput = document.getElementById('quarterInput')?.value;

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

    UIkit.notification({
        message: `🔍 Проверка делян по ${typeNames[type]}: "${name}"...`,
        status: 'primary',
        timeout: 3000
    });

    fetch('/api/plots/validate-by-territory?type=' + type + '&id=' + id, {
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
// ПРОВЕРКА ВСЕХ ДЕЛЯН (ГЛОБАЛЬНО)
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
        loadAllPlots();
    }

    UIkit.notification({
        message: showLabels ? '✅ Метки включены' : '❌ Метки скрыты',
        status: showLabels ? 'success' : 'warning',
        timeout: 1500
    });
}

function loadRegions() {
    const regionSelect = document.getElementById('regionSelect');
    if (!regionSelect) return;

    if (regionSelect.options.length > 1) {
        console.log('✅ Регионы уже загружены');
        return;
    }

    console.log('📥 Загружаем список регионов...');

    fetch('/api/regions')
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

            const uiRegionId = document.getElementById('uiRegionId')?.value;
            if (uiRegionId) {
                regionSelect.value = uiRegionId;
                onRegionChange(uiRegionId);
            }
        })
        .catch(error => {
            console.error('❌ Ошибка загрузки регионов:', error);
            regionSelect.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
        });
}

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
