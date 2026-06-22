// ==========================================
// РАБОТА С КООРДИНАТАМИ
// ==========================================

function addCoordinate() {
    const container = document.getElementById('coordinates-container');
    const index = container.children.length;

    const div = document.createElement('div');
    div.className = 'coordinate-row';
    div.id = 'coord-row-' + index;
    div.innerHTML = `
        <input class="uk-input uk-form-width-small coord-input" type="text"
               name="coordinates[${index}].lat"
               placeholder="Широта"
               oninput="validateCoordInput(this)">
        <input class="uk-input uk-form-width-small coord-input" type="text"
               name="coordinates[${index}].lng"
               placeholder="Долгота"
               oninput="validateCoordInput(this)">
        <button type="button" class="uk-button uk-button-danger uk-button-small"
                onclick="removeCoordinate(${index})">
            <span uk-icon="icon: close"></span>
        </button>
    `;
    container.appendChild(div);

    if (window.UIkit) {
        UIkit.icon(div);
    }
}

function removeCoordinate(index) {
    const container = document.getElementById('coordinates-container');
    const element = document.getElementById('coord-row-' + index);

    if (element && container.children.length > 3) {
        element.remove();
    } else {
        UIkit.notification({
            message: 'Нельзя удалить последнюю точку (нужно минимум 3)',
            status: 'warning',
            timeout: 2000
        });
    }
}

function clearAllCoordinates() {
    const container = document.getElementById('coordinates-container');
    while (container.children.length > 3) {
        container.removeChild(container.lastChild);
    }
    container.querySelectorAll('input').forEach(input => input.value = '');
}

function resetForm() {
    clearAllCoordinates();
    document.getElementById('numberInQuarter').value = '';
    document.getElementById('plots').value = '';
    document.getElementById('description').value = '';
    document.getElementById('yearOfCut').value = '';
    document.getElementById('cutType').value = '';

    // Сбрасываем все селекты
    resetAllDependentSelects();

    const regionSelect = document.getElementById('regionSelect');
    regionSelect.value = '';
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

    // === 1. Регион ===
    const regionSelect = document.getElementById('regionSelect');
    regionSelect.value = regionId;

    if (centerLat && centerLng && map) {
        map.setView([parseFloat(centerLat), parseFloat(centerLng)], parseInt(zoom) || 7);
    }

    // === 2. Район ===
    loadMunicipalDistricts(regionId, function() {
        if (municipalDistrictId && municipalDistrictId !== '') {
            const districtSelect = document.getElementById('municipalDistrictSelect');
            districtSelect.value = municipalDistrictId;

            // === 3. Лесничество ===
            loadForestries(municipalDistrictId, function() {
                if (forestryId && forestryId !== '') {
                    const forestrySelect = document.getElementById('forestrySelect');
                    forestrySelect.value = forestryId;

                    // === 4. Участковое ===
                    loadDistrictForestries(forestryId, function() {
                        if (districtForestryId && districtForestryId !== '') {
                            const districtSelect2 = document.getElementById('districtForestrySelect');
                            districtSelect2.value = districtForestryId;

                            // === 5. Техучасток ===
                            loadTechnicalUnits(districtForestryId, function() {
                                if (technicalUnitId && technicalUnitId !== '') {
                                    const techSelect = document.getElementById('technicalUnitSelect');
                                    techSelect.value = technicalUnitId;

                                    // === 6. Квартал ===
                                    loadQuarters(technicalUnitId, function() {
                                        if (quarterId && quarterId !== '') {
                                            fetch('/api/quarters/' + quarterId)
                                                .then(response => response.json())
                                                .then(quarter => {
                                                    if (quarter && quarter.number) {
                                                        const input = document.getElementById('quarterInput');
                                                        input.value = 'Кв. ' + quarter.number + (quarter.name ? ' (' + quarter.name + ')' : '');
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
    // Сбрасываем ВСЕ зависимые селекты
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
    // Муниципальный район
    const districtSelect = document.getElementById('municipalDistrictSelect');
    districtSelect.innerHTML = '<option value="">-- Сначала выберите регион --</option>';
    districtSelect.disabled = true;
    districtSelect.classList.remove('loading');

    // Лесничество
    const forestrySelect = document.getElementById('forestrySelect');
    forestrySelect.innerHTML = '<option value="">-- Сначала выберите район --</option>';
    forestrySelect.disabled = true;
    forestrySelect.classList.remove('loading');

    // Участковое лесничество
    const districtForestrySelect = document.getElementById('districtForestrySelect');
    districtForestrySelect.innerHTML = '<option value="">-- Сначала выберите лесничество --</option>';
    districtForestrySelect.disabled = true;
    districtForestrySelect.classList.remove('loading');

    // Технический участок
    const techSelect = document.getElementById('technicalUnitSelect');
    techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
    techSelect.disabled = true;
    techSelect.classList.remove('loading');

    // Квартал
    document.getElementById('quarterInput').disabled = true;
    document.getElementById('quarterInput').value = '';
    document.getElementById('quarterId').value = '';
    document.getElementById('quarterSuggestions').style.display = 'none';
}

// ==========================================
// ЗАГРУЗКА ЗАВИСИМЫХ СПИСКОВ
// ==========================================

function loadMunicipalDistricts(regionId, callback) {
    if (!regionId) {
        resetAllDependentSelects();
        if (callback) callback();
        return;
    }

    const select = document.getElementById('municipalDistrictSelect');
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
    if (!technicalUnitId) {
        document.getElementById('quarterInput').disabled = true;
        document.getElementById('quarterInput').value = '';
        document.getElementById('quarterId').value = '';
        document.getElementById('quarterSuggestions').style.display = 'none';
        if (callback) callback();
        return;
    }

    document.getElementById('quarterInput').disabled = false;
    document.getElementById('quarterInput').placeholder = 'Введите номер квартала...';
    if (callback) callback();
}

// ==========================================
// КВАРТАЛЫ (AUTOCOMPLETE)
// ==========================================

let quarterSearchTimeout = null;

function searchQuarters(query) {
    const technicalUnitId = document.getElementById('technicalUnitSelect').value;
    const suggestionsDiv = document.getElementById('quarterSuggestions');
    const quarterInput = document.getElementById('quarterInput');

    if (!technicalUnitId) {
        suggestionsDiv.innerHTML = '<div class="no-results">Сначала выберите технический участок</div>';
        suggestionsDiv.style.display = 'block';
        quarterInput.disabled = true;
        return;
    }

    quarterInput.disabled = false;

    if (!query || query.trim().length === 0) {
        suggestionsDiv.style.display = 'none';
        return;
    }

    clearTimeout(quarterSearchTimeout);
    quarterSearchTimeout = setTimeout(() => {
        suggestionsDiv.innerHTML = '<div class="loading-suggestions">Поиск...</div>';
        suggestionsDiv.style.display = 'block';

        fetch('/api/quarters/search?technicalUnitId=' + technicalUnitId + '&query=' + encodeURIComponent(query.trim()))
            .then(response => {
                if (!response.ok) throw new Error('HTTP ' + response.status);
                return response.json();
            })
            .then(data => {
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
                suggestionsDiv.innerHTML = '<div class="no-results">❌ Ошибка загрузки</div>';
            });
    }, 300);
}

function selectQuarter(id, number, name) {
    const quarterInput = document.getElementById('quarterInput');
    const quarterIdInput = document.getElementById('quarterId');
    const suggestionsDiv = document.getElementById('quarterSuggestions');

    quarterInput.value = 'Кв. ' + number + (name ? ' (' + name + ')' : '');
    quarterIdInput.value = id;
    suggestionsDiv.style.display = 'none';

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
        districtSelect.innerHTML = '<option value="">-- Сначала выберите лесничество --</option>';
        districtSelect.disabled = true;
    }
    if (!level || level === 'technical' || level === 'quarter') {
        const techSelect = document.getElementById('technicalUnitSelect');
        techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
        techSelect.disabled = true;
    }
    if (!level || level === 'quarter') {
        document.getElementById('quarterInput').disabled = true;
        document.getElementById('quarterInput').value = '';
        document.getElementById('quarterId').value = '';
        document.getElementById('quarterSuggestions').style.display = 'none';
    }
}

// Закрываем подсказки при клике вне
document.addEventListener('click', function(e) {
    const container = document.getElementById('quarterInput')?.closest('.autocomplete-wrapper');
    if (container && !container.contains(e.target)) {
        document.getElementById('quarterSuggestions').style.display = 'none';
    }
});

// ==========================================
// КАРТА
// ==========================================

let map = null;

document.addEventListener('DOMContentLoaded', function() {
    try {
        map = L.map('map').setView([56.0, 92.0], 6);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors'
        }).addTo(map);

        loadUISettingsFromServer();
        loadPlots();

        const regionSelect = document.getElementById('regionSelect');
        const options = regionSelect.querySelectorAll('option');
        const regionOptions = Array.from(options).filter(opt => opt.value !== '');

        if (regionOptions.length === 1 && !regionSelect.value) {
            const regionId = regionOptions[0].value;
            regionSelect.value = regionId;
            onRegionChange(regionId);
        }
    } catch (e) {
        console.error('Ошибка инициализации карты:', e);
    }
});

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

            plots.forEach(plot => {
                if (plot.geometryGeoJson) {
                    try {
                        const geojson = JSON.parse(plot.geometryGeoJson);
                        if (geojson.type === 'Polygon' && geojson.coordinates) {
                            const coords = geojson.coordinates[0].map(c => [c[1], c[0]]);
                            const polygon = L.polygon(coords, {
                                color: plot.verified ? '#2e7d32' : '#f57c00',
                                weight: 2,
                                opacity: 0.8,
                                fillOpacity: 0.3
                            }).addTo(map);

                            polygon.bindPopup(`
                                <b>${plot.fullNumber || plot.numberInQuarter}</b><br>
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

function checkAll() {
    UIkit.notification({
        message: 'Запуск проверки всех делян...',
        status: 'primary',
        timeout: 2000
    });

    fetch('/api/plots/validate-all', {
        method: 'POST'
    }).then(response => {
        if (response.ok) {
            window.location.reload();
        } else {
            UIkit.notification({
                message: 'Ошибка при проверке',
                status: 'danger',
                timeout: 3000
            });
        }
    }).catch(error => {
        console.error('Error:', error);
        UIkit.notification({
            message: 'Ошибка: ' + error.message,
            status: 'danger',
            timeout: 3000
        });
    });
}
