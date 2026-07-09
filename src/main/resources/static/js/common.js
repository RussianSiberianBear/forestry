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