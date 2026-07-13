function getDefaultCoordinateCenterMap(){
    // пока возвращаем по умолчанию координаты с.Бичура
    return [50.592834,107.598389];
}

// ==========================================
// КАРТА
// ==========================================

let map = null;
let osmLayer = null;
let googleSatLayer = null;
function initMap(element,centerCoordinates, zoom) {
    try {
        if (document.getElementById(element)) {
            map = L.map(element).setView(centerCoordinates, zoom);

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
                "🛰️ Спутник": googleSatLayer,
                "🗺️ Схема": osmLayer
            };

            googleSatLayer.addTo(map);
            L.control.layers(baseMaps).addTo(map);

            loadUISettingsFromServer();

            updateCoordCounter();
            updateTerritoryInfo();

            console.log('✅ Карта инициализирована');
        } else {
            console.warn('⚠️ Элемент #'+element+' не найден на странице');
        }
    } catch (e) {
        console.error('❌ Ошибка инициализации карты:', e);
    }
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

/**
 *
 * @param arr массив объектов
 * @param id искомый идентификатор объекта
 * @returns {*|null} найденный объект или null
 */
function findById(arr, id) {
    // Проверяем, что arr - это массив
    if (!Array.isArray(arr)) {
        console.error('Первый аргумент должен быть массивом');
        return null;
    }
    const found = arr.find(item => item.id == id);
    return found || null; // или undefined
}

function getCurrentForestry(){
    const forestrySelect = document.getElementById('forestrySelect');
    const forestryId = forestrySelect.value;
    if (forestryId && forestries){
        return findById(forestries,forestryId);
    }
    return null;
}

document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('[data-toggle="filter"]').forEach(toggle => {
        toggle.addEventListener('click', function () {
            const targetSelector = this.getAttribute('data-target');
            const targetElement = document.querySelector(targetSelector);
            if (targetElement) {
                // Переключаем класс
                targetElement.classList.toggle('uk-hidden');
                // Меняем стрелку
                const arrow = this.querySelector('.filter-arrow') || this;
                arrow.textContent = arrow.textContent.includes('▲')
                    ? arrow.textContent.replace('▲', '▼')
                    : arrow.textContent.replace('▼', '▲');
            }
        });
    });

});

function resetFilterUI(root) {
    root = root?.root ?? root;
    if (typeof root === 'string') root = document.getElementById(root) || document;
    if (!root || !root.querySelectorAll) root = document;

    root.querySelectorAll('[data-filter]').forEach(el => {
        const tag = el.tagName.toLowerCase();
        const type = (el.type || '').toLowerCase();

        if (tag === 'select') el.value = '';
        else if (type === 'checkbox') el.checked = false;
        else if (type === 'radio') el.checked = false;
        else el.value = '';
    });
}

function collectFilterAuto(root, opts = {}) {
    root = root?.root ?? root;
    if (typeof root === 'string') root = document.getElementById(root) || document;
    if (!root || !root.querySelectorAll) root = document;

    const {
        selector = '[data-filter]',
        trim = true,
        skipEmpty = true,
        skipDisabled = true
    } = opts;

    const filter = {};
    const elements = root.querySelectorAll(selector);

    elements.forEach(el => {
        if (skipDisabled && el.disabled) return;

        const key = el.dataset.filterKey || el.name || el.id;
        if (!key) return;

        let v;

        const tag = el.tagName.toLowerCase();
        const type = (el.type || '').toLowerCase();

        if (tag === 'select') {
            if (el.multiple) {
                v = Array.from(el.selectedOptions).map(o => o.value).filter(x => x !== '');
            } else {
                v = el.value;
            }
        } else if (type === 'checkbox') {
            v = el.checked;
            // обычно false не шлём, чтобы “не фильтровать”
            if (skipEmpty && v === false) return;
        } else if (type === 'radio') {
            const checked = root.querySelector(`input[type="radio"][name="${CSS.escape(el.name)}"]:checked`);
            v = checked ? checked.value : '';
        } else {
            v = el.value;
        }

        if (typeof v === 'string' && trim) v = v.trim();

        // парсинг по data-filter-type
        const hint = (el.dataset.filterType || '').toLowerCase();
        if (hint === 'number' && typeof v === 'string' && v !== '') {
            const n = Number(v);
            if (!Number.isNaN(n)) v = n;
        } else if (hint === 'boolean' && typeof v === 'string') {
            if (v === 'true') v = true;
            if (v === 'false') v = false;
        }

        if (skipEmpty) {
            if (v === '' || v === null || v === undefined) return;
            if (Array.isArray(v) && v.length === 0) return;
        }

        filter[key] = v;
    });

    return filter;
}

const pluralRu = (n, one, few, many) => {
    n = Math.abs(n) % 100;
    const n1 = n % 10;
    if (n > 10 && n < 20) return many;
    if (n1 > 1 && n1 < 5) return few;
    if (n1 === 1) return one;
    return many;
};

async function deleteWithConfirmEx(grid, options = {}) {

    const result = await grid.confirmEx(
        options.query,
        'Подтверждение',
        {variant: 'danger'}
    );
    if (result.value != 'yes') return false;

    grid.loading('Очистка...');
    try {
        const res = await grid.http.request({url: options.url, method: 'POST', strict: false, unwrapData: false});
        const cnt = Number(res?.data?.count);
        let msg =  res?.message ?? '';

        if (cnt === 0) msg = options.noData;
        if (Number.isFinite(cnt) && cnt > 0) {
            msg += ` Удалено: ${cnt} ${pluralRu(cnt, 'запись', 'записи', 'записей')}.`;
        }
        grid.toast(msg, 'success');
        if (res?.success === true && cnt > 0 ) await grid.load();

    } catch (e) {
        grid.toast('Ошибка при удалении!', 'error');
    } finally {
        grid.loading.hide();
    }
    return false;
}

async function bulkOperation(action, ctx, grid, opt = {}) {
    let operation = grid.toolbar.getValue(opt.action);
    if (!operation) {
        grid.toast('Выберите операцию!', 'error');
        return false;
    }
    var rowIds = grid.getCheckedRowIds();
    if (!rowIds.length) {
        grid.toast('Нет выбранных строк', 'error');
        return false;
    }
    const result = await grid.confirmEx(
        opt.qConfirmEx,
        'Подтверждение',
        {variant: 'danger'}
    );
    if (result.value != 'yes') return false;
    const requestData = {['oper']: opt['operName'], rowIds};
    if (opt.hasOwnProperty('data') && !(Object.keys(opt.data).length === 0)) requestData.data = opt.data;
    grid.loading('Выполнение...');
    try {
        const res = await grid.data.request({
            requestData,
            strict: false,
            unwrapData: false,
            intent: opt.intent + operation
        });
        const cnt = Number(res?.data?.count);
        let msg = res?.message ?? '';
        if (cnt === 0) msg = 'Строки не найдены!';
        if (Number.isFinite(cnt) && cnt > 0) {
            msg += ` Помечено: ${cnt} ${pluralRu(cnt, 'запись', 'записи', 'записей')}.`;
        }
        if (res?.success === true) {
            grid.toast(msg, 'success');
            grid.load();
        } else if (res?.success === false) grid.toast(res?.message ?? 'Ошибка при выполнении операции!', 'error');
    } catch (e) {
        grid.toast('Ошибка при выполнении операции!', 'error');
    } finally {
        grid.loading.hide();
    }
    return false;
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function setMonthPeriod(from, to) {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    // Первый день месяца
    const fromDate = new Date(year, month, 1);
    // Последний день месяца
    const toDate = new Date(year, month + 1, 0);
    // Форматируем и устанавливаем
    document.getElementById(from).value = formatDate(fromDate);
    document.getElementById(to).value = formatDate(toDate);
}

function setQuarterPeriod(from,to) {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();

    // Определяем начало квартала (0-2: Q1, 3-5: Q2, 6-8: Q3, 9-11: Q4)
    const quarterStartMonth = Math.floor(month / 3) * 3;

    // Первый день квартала
    const fromDate = new Date(year, quarterStartMonth, 1);

    // Последний день квартала
    const toDate = new Date(year, quarterStartMonth + 3, 0);

    document.getElementById(from).value = formatDate(fromDate);
    document.getElementById(to).value = formatDate(toDate);
}

function setYearPeriod(from,to) {
    const now = new Date();
    const year = now.getFullYear();

    // Первый день года (1 января)
    const fromDate = new Date(year, 0, 1);

    // Последний день года (31 декабря)
    const toDate = new Date(year, 11, 31);

    document.getElementById(from).value = formatDate(fromDate);
    document.getElementById(to).value = formatDate(toDate);
}

async function reportDownload(grid, payload, downloadUrl, filename) {
    grid.loading('Выполнение...');
    try {
        const response = await fetch(downloadUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'same-origin',
            body: JSON.stringify(payload)
        });

        if (await grid.handleUnauthorized(response)) {
            return false;
        }

        if (!response.ok) {
            let errorMessage = 'Не удалось скачать отчет';

            const contentType = response.headers.get('content-type') || '';

            if (contentType.includes('application/json')) {
                const errorJson = await response.json();
                errorMessage = errorJson.message || errorJson.error || errorMessage;
            } else {
                const errorText = await response.text();
                if (errorText) {
                    errorMessage = errorText;
                }
            }

            throw new Error(errorMessage);
        }

        const blob = await response.blob();

        const contentDisposition = response.headers.get('content-disposition');
        if (contentDisposition) {
            const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;\n]+)/i);
            if (utf8Match && utf8Match[1]) {
                filename = decodeURIComponent(utf8Match[1]);
            } else {
                const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/i);
                if (filenameMatch && filenameMatch[1]) {
                    filename = filenameMatch[1].replace(/['"]/g, '');
                }
            }
        }

        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', filename || 'report');

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

    } catch (e) {
        console.error('Ошибка при скачивании отчета:', e);
        grid.toast(e.message || 'Не удалось скачать отчет', 'error');
    } finally {
        grid.loading.hide();
    }

    return false;
}


// Глобальная политика
// ВАЖНО: вызвать ДО new ABGrid(...)
ABGrid.setDefaults({
    auth: {
        redirectOn401: true,
        loginUrl: '/auth/login',
        redirectDelayMs: 3000,
        redirectOnlyOnce: true,
        showToast: true
    },
    debug: {enabled: true}
});



