// ==========================================
// ЗАГРУЗКА СПИСКОВ (С DTO)
// ==========================================

function loadForestries(callback) {
    const forestrySelect = document.getElementById('forestrySelect');
    if (!forestrySelect) return;

    if (forestrySelect.options.length > 1) {
        console.log('✅ Лесничества уже загружены');
        if (callback) callback();
        return;
    }

    console.log('📥 Загружаем список лесничеств...');

    forestrySelect.classList.add('loading');
    forestrySelect.innerHTML = '<option value="">Загрузка...</option>';
    forestrySelect.disabled = true;

    fetch('/api/forestry/forestries/all')
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            forestrySelect.innerHTML = '';
            if (Array.isArray(data) && data.length > 0) {
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = '-- Выберите лесничество --';
                forestrySelect.appendChild(defaultOption);

                data.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.id;
                    option.textContent = item.name || 'Без названия';
                    forestrySelect.appendChild(option);
                });
                forestrySelect.disabled = false;

                if (data.length === 1) {
                    forestrySelect.value = data[0].id;
                    saveUISetting('forestry', data[0].id);
                    enableQuarterField();
                    loadSubForestries(data[0].id);
                }
            } else {
                forestrySelect.innerHTML = '<option value="">-- Лесничества не найдены --</option>';
                forestrySelect.disabled = true;
            }
            forestrySelect.classList.remove('loading');
            updateTerritoryInfo();
            if (callback) callback();
        })
        .catch(error => {
            console.error('❌ Ошибка загрузки лесничеств:', error);
            forestrySelect.innerHTML = '<option value="">❌ Ошибка загрузки</option>';
            forestrySelect.classList.remove('loading');
            if (callback) callback();
            UIkit.notification({
                message: 'Ошибка загрузки лесничеств',
                status: 'danger',
                timeout: 3000
            });
        });
}

function loadSubForestries(forestryId, callback) {
    if (!forestryId) {
        resetDependentSelects('district');
        if (callback) callback();
        return;
    }

    const select = document.getElementById('subForestrySelect');
    if (!select) return;

    select.classList.add('loading');
    select.innerHTML = '<option value="">Загрузка...</option>';
    select.disabled = true;

    fetch('/api/forestry/sub-forestries/by-forestry/' + forestryId)
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

    fetch('/api/forestry/technical-units/by-district/' + districtForestryId)
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
    const subForestryId = document.getElementById('subForestrySelect')?.value;
    const forestryId = document.getElementById('forestrySelect')?.value;
    const suggestionsDiv = document.getElementById('quarterSuggestions');
    const quarterInput = document.getElementById('quarterInput');

    if (!quarterInput) return;

    let parentId = technicalUnitId || subForestryId || forestryId;

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

        const url = '/api/forestry/quarters/search?technicalUnitId=' + parentId + '&query=' + encodeURIComponent(query.trim());

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

function selectQuarter(id, number, name) {
    const quarterInput = document.getElementById('quarterInput');
    const quarterIdInput = document.getElementById('quarterId');
    const suggestionsDiv = document.getElementById('quarterSuggestions');
    const numberInQuarterInput = document.getElementById('numberInQuarter');

    if (quarterInput) {
        quarterInput.value = 'Кв. ' + number + (name ? ' (' + name + ')' : '');
        quarterInput.disabled = false;
    }
    if (quarterIdInput) quarterIdInput.value = id;
    if (suggestionsDiv) suggestionsDiv.style.display = 'none';

    if (numberInQuarterInput) {
        numberInQuarterInput.disabled = false;
        numberInQuarterInput.placeholder = 'Введите номер деляны...';
        numberInQuarterInput.focus();
    }

    saveUISetting('forestry-unit', id);
    saveUISetting('forestry-type', 'QUARTER');
    updateTerritoryInfo();

    UIkit.notification({
        message: '✅ Выбран квартал ' + number,
        status: 'success',
        timeout: 1500
    });
}

// ==========================================
// ОБРАБОТЧИКИ ИЗМЕНЕНИЙ
// ==========================================

function onForestryChange(forestryId) {
    resetDependentSelects('district');

    if (!forestryId) {
        saveUISetting('forestry-unit', 0);
        saveUISetting('forestry-type', '');
        const quarterInput = document.getElementById('quarterInput');
        if (quarterInput) {
            quarterInput.disabled = true;
            quarterInput.value = '';
            quarterInput.placeholder = 'Сначала выберите лесничество';
        }
        const numberInQuarterInput = document.getElementById('numberInQuarter');
        if (numberInQuarterInput) {
            numberInQuarterInput.disabled = true;
            numberInQuarterInput.value = '';
            numberInQuarterInput.placeholder = 'Сначала выберите квартал';
        }
        return;
    }

    const quarterInput = document.getElementById('quarterInput');
    if (quarterInput) {
        quarterInput.disabled = false;
        quarterInput.placeholder = 'Введите номер квартала...';
    }

    saveUISetting('forestry-unit', forestryId);
    saveUISetting('forestry-type', 'FORESTRY');
    loadSubForestries(forestryId);

    updateTerritoryInfo();
}

function onSubForestryChange(subForestryId) {
    console.log('🔄 onSubForestryChange вызван с subForestryId:', subForestryId);

    const techSelect = document.getElementById('technicalUnitSelect');
    const quarterInput = document.getElementById('quarterInput');

    if (!subForestryId) {
        if (techSelect) {
            techSelect.innerHTML = '<option value="">-- Сначала выберите участковое лесничество --</option>';
            techSelect.disabled = true;
        }
        saveUISetting('forestry-unit', 0);
        saveUISetting('forestry-type', '');
        return;
    }

    if (techSelect) {
        techSelect.innerHTML = '<option value="">Загрузка...</option>';
        techSelect.disabled = true;
    }

    saveUISetting('forestry-unit', subForestryId);
    saveUISetting('forestry-type', 'SUB_FORESTRY');

    loadTechnicalUnits(subForestryId);

    updateTerritoryInfo();
}

function onTechnicalUnitChange(technicalUnitId) {
    const quarterInput = document.getElementById('quarterInput');

    if (!technicalUnitId) {
        saveUISetting('forestry-unit', 0);
        saveUISetting('forestry-type', '');
        return;
    }

    saveUISetting('forestry-unit', technicalUnitId);
    saveUISetting('forestry-type', 'TECHNICAL_UNIT');

    updateTerritoryInfo();
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
    const forestryId = document.getElementById('forestrySelect')?.value;
    const subForestryId = document.getElementById('subForestrySelect')?.value;
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
    } else if (subForestryId && subForestryId !== '') {
        const select = document.getElementById('subForestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Участковое лесничество';
    } else if (forestryId && forestryId !== '') {
        const select = document.getElementById('forestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Лесничество';
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
    const forestryId = document.getElementById('forestrySelect')?.value;
    const subForestryId = document.getElementById('subForestrySelect')?.value;
    const technicalUnitId = document.getElementById('technicalUnitSelect')?.value;
    const quarterId = document.getElementById('quarterId')?.value;
    const numberInQuarter = document.getElementById('numberInQuarter')?.value?.trim();

    let type = null;
    let id = null;
    let name = '';

    if (quarterId && quarterId !== '') {
        type = 'FOREST_QUARTER';
        id = quarterId;
        name = document.getElementById('quarterInput')?.value || 'Квартал';
    } else if (technicalUnitId && technicalUnitId !== '') {
        type = 'TECHNICAL_UNIT';
        id = technicalUnitId;
        const select = document.getElementById('technicalUnitSelect');
        name = select?.options[select.selectedIndex]?.text || 'Технический участок';
    } else if (subForestryId && subForestryId !== '') {
        type = 'SUB_FORESTRY';
        id = subForestryId;
        const select = document.getElementById('subForestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Участковое лесничество';
    } else if (forestryId && forestryId !== '') {
        type = 'FORESTRY';
        id = forestryId;
        const select = document.getElementById('forestrySelect');
        name = select?.options[select.selectedIndex]?.text || 'Лесничество';
    } else {
        UIkit.notification({
            message: '❌ Выберите лесничество в форме выше',
            status: 'warning',
            timeout: 4000
        });
        return;
    }

    const typeNames = {
        'FORESTRY': 'лесничеству',
        'SUB_FORESTRY': 'участковому лесничеству',
        'TECHNICAL_UNIT': 'техническому участку',
        'FOREST_QUARTER': 'кварталу'
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

    fetch('/api/cutting-area/validate-by-territory?' + params.toString(), {
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
// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ==========================================

function resetDependentSelects(level) {
    if (!level || level === 'district') {
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

    const numberInQuarterInput = document.getElementById('numberInQuarter');
    if (numberInQuarterInput) {
        numberInQuarterInput.disabled = true;
        numberInQuarterInput.value = '';
        numberInQuarterInput.placeholder = 'Сначала выберите квартал';
    }
}

function resetAllDependentSelects() {
    const subForestrySelect = document.getElementById('subForestrySelect');
    if (subForestrySelect) {
        subForestrySelect.innerHTML = '<option value="">-- Сначала выберите лесничество --</option>';
        subForestrySelect.disabled = true;
        subForestrySelect.classList.remove('loading');
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

    const numberInQuarterInput = document.getElementById('numberInQuarter');
    if (numberInQuarterInput) {
        numberInQuarterInput.disabled = true;
        numberInQuarterInput.value = '';
        numberInQuarterInput.placeholder = 'Сначала выберите квартал';
    }

    updateTerritoryInfo();
}

document.addEventListener('click', function(e) {
    const container = document.getElementById('quarterInput')?.closest('.autocomplete-wrapper');
    if (container && !container.contains(e.target)) {
        const suggestions = document.getElementById('quarterSuggestions');
        if (suggestions) suggestions.style.display = 'none';
    }
});



