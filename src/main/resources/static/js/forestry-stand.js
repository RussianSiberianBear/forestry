// ==========================================
// ИНИЦИАЛИЗАЦИЯ
// ==========================================

document.addEventListener('DOMContentLoaded', function () {
    loadForestries();

    const numberInQuarterInput = document.getElementById('numberInQuarter');
    if (numberInQuarterInput) {
        numberInQuarterInput.addEventListener('input', function () {
            updateTerritoryInfo();
        });
    }
    const selectors = ['forestrySelect', 'subForestrySelect', 'technicalUnitSelect'];
    selectors.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('change', function () {
                updateTerritoryInfo();
            });
        }
    });
}