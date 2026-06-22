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
                                                    }
                                                })
                                                .catch(error => console.error('Ошибка загрузки квартала:', error));
                                        }
                                    });
                                } else {
                                    loadQuarters(districtForestryId);
                                }
                            });
                        } else {
                            loadTechnicalUnits(districtForestryId);
                        }
                    });
                } else {
                    loadDistrictForestries(forestryId);
                }
            });
        } else {
            loadForestries(municipalDistrictId);
        }
    });
}

// ==========================================
// ОБРАБОТЧИКИ ИЗМЕНЕНИЙ
// ==========================================

function onRegionChange(regionId) {
    resetAllDependentSelects();

    if (!regionId) {
        return;
    }

    saveUISetting('region', regionId);
    loadMunicipalDistricts(regionId);
    saveUISetting('municipal-district', 0);
    saveUISetting('forestry', 0);
    saveUISetting('district-forestry', 0);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);
}

function onMunicipalDistrictChange(municipalDistrictId) {
    resetDependentSelects('forestry');

    if (!municipalDistrictId) {
        return;
    }

    saveUISetting('municipal-district', municipalDistrictId);
    loadForestries(municipalDistrictId);
    saveUISetting('forestry', 0);
    saveUISetting('district-forestry', 0);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);
}

function onForestryChange(forestryId) {
    resetDependentSelects('district');

    if (!forestryId) {
        return;
    }

    saveUISetting('forestry', forestryId);
    loadDistrictForestries(forestryId);
    saveUISetting('district-forestry', 0);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);
}

function onDistrictForestryChange(districtForestryId) {
    resetDependentSelects('technical');

    if (!districtForestryId) {
        return;
    }

    saveUISetting('district-forestry', districtForestryId);
    loadTechnicalUnits(districtForestryId);
    saveUISetting('technical-unit', 0);
    saveUISetting('quarter', 0);
}

function onTechnicalUnitChange(technicalUnitId) {
    resetDependentSelects('quarter');

    if (!technicalUnitId) {
        return;
    }

    saveUISetting('technical-unit', technicalUnitId);
    loadQuarters(technicalUnitId);
    saveUISetting('quarter', 0);
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
    if (!districtForestryId) {
        resetDependentSelects('technical');
        if (callback) callback();
        return;
    }

    const select = document.getElementById('technicalUnitSelect');
    if (!select) return;

    select.classList.add('loading');
    select.innerHTML = '<option value="">Загрузка...</option>';
    select.disabled = true;

    fetch('/api/technical-units/by-district/' + districtForestryId)
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(data => {
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
            if (callback) callback();
        })
        .catch(error => {
            console.error('Ошибка загрузки технических участков:', error);
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

    UIkit.notification({
        message: '✅ Выбран квартал ' + number,
        status: 'success',
        timeout: 1500
    });
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ==========================================

function resetDependentSelects(level) {
    if (!level || level === 'district' || level === 'technical' || level === 'quarter') {
        const districtSelect = document.getElementById('districtForestrySelect');
        if (districtSelect) {
            districtSelect.innerHTML = '<option value="">-- Сначала выберите лесничество --</option>';
            districtSelect.disabled = true;
        }
    }
    if (!level || level === 'technical' || level === 'quarter') {
        const techSelect = document.getElementById('technicalUnitSelect');
        if (techSelect) {
            techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
            techSelect.disabled = true;
        }
    }
    if (!level || level === 'quarter') {
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
// КАРТА
// ==========================================

let map = null;
let osmLayer = null;
let googleSatLayer = null;

document.addEventListener('DOMContentLoaded', function() {
    try {
        map = L.map('map').setView([56.0, 92.0], 6);

        // ===== СЛОЙ OSM (СХЕМА) =====
        osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 19
        });

        // ===== СЛОЙ GOOGLE SATELLITE (СПУТНИК) =====
        googleSatLayer = L.tileLayer('https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            attribution: '© Google Maps'
        });

        // ===== ДОБАВЛЯЕМ СЛОИ И ПЕРЕКЛЮЧАТЕЛЬ =====
        var baseMaps = {
            "🗺️ Схема": osmLayer,
            "🛰️ Спутник": googleSatLayer
        };

        // ✅ По умолчанию показываем СПУТНИК
        googleSatLayer.addTo(map);
        L.control.layers(baseMaps).addTo(map);

        // Загружаем настройки и деляны
        loadUISettingsFromServer();
        loadPlots();

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
    } catch (e) {
        console.error('Ошибка инициализации карты:', e);
    }
});

// ==========================================
// ЗАГРУЗКА ДЕЛЯН НА КАРТУ
// ==========================================

function loadPlots() {
    fetch('/api/plots/map-data')
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(plots => {
            if (!map) return;

            // Удаляем старые полигоны (сохраняем только базовые слои)
            map.eachLayer(function(layer) {
                if (layer instanceof L.Polygon) {
                    map.removeLayer(layer);
                }
            });

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
                            }).addTo(map);

                            polygon.bindPopup(`
                                <b style="color: #d32f2f;">${plot.fullNumber || plot.numberInQuarter}</b><br>
                                ${plot.forestryName || 'Без лесничества'}<br>
                                ${plot.verified ? '✅ Верифицирована' : '⏳ Не проверена'}<br>
                                <small>Площадь: ${plot.areaM2 ? (plot.areaM2 / 10000).toFixed(2) + ' га' : 'н/д'}</small>
                            `);
                        }
                    } catch(e) {
                        console.error('Ошибка при отображении деляны:', plot.fullNumber, e);
                    }
                }
            });
        })
        .catch(error => console.error('Error loading plots:', error));
}

// ==========================================
// ПРОВЕРКА ВСЕХ ДЕЛЯН
// ==========================================

// ==========================================
// ПРОВЕРКА ВСЕХ ДЕЛЯН (БЕЗ СБРОСА КАРТЫ)
// ==========================================

// ==========================================
// ПРОВЕРКА ВСЕХ ДЕЛЯН (БЕЗ КАКИХ-ЛИБО ИЗМЕНЕНИЙ КАРТЫ)
// ==========================================

function checkAll() {
    UIkit.notification({
        message: '🔍 Запуск проверки всех делян...',
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

                // Скрываем блок конфликтов, если он был открыт
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
