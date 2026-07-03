// ==========================================
// РАБОТА С ФАЙЛАМИ
// ==========================================

const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const fileName = document.getElementById('fileName');
const uploadBtn = document.getElementById('uploadBtn');
const progressBar = document.getElementById('progressBar');
const uploadProgress = document.getElementById('uploadProgress');
const progressText = document.getElementById('progressText');

dropZone.addEventListener('click', function (e) {
    if (e.target.tagName !== 'BUTTON') {
        fileInput.click();
    }
});

fileInput.addEventListener('change', function (e) {
    if (this.files && this.files[0]) {
        handleFile(this.files[0]);
    }
});

dropZone.addEventListener('dragover', function (e) {
    e.preventDefault();
    this.classList.add('dragover');
});

dropZone.addEventListener('dragleave', function (e) {
    e.preventDefault();
    this.classList.remove('dragover');
});

dropZone.addEventListener('drop', function (e) {
    e.preventDefault();
    this.classList.remove('dragover');

    const files = e.dataTransfer.files;
    if (files && files[0]) {
        const file = files[0];
        const ext = file.name.split('.').pop().toLowerCase();
        if (ext === 'xlsx' || ext === 'xls') {
            fileInput.files = files;
            handleFile(file);
        } else {
            UIkit.notification({
                message: 'Загрузите файл Excel (.xlsx или .xls)',
                status: 'danger',
                timeout: 3000
            });
        }
    }
});

function handleFile(file) {
    fileName.textContent = file.name + ' (' + (file.size / 1024).toFixed(1) + ' KB)';
    uploadBtn.disabled = false;
    dropZone.style.borderColor = '#1e87f0';

    UIkit.notification({
        message: 'Файл "' + file.name + '" выбран',
        status: 'success',
        timeout: 2000
    });
}

function resetUpload() {
    fileInput.value = '';
    fileName.textContent = 'Файл не выбран';
    uploadBtn.disabled = true;
    dropZone.style.borderColor = '#ccc';
    progressBar.classList.remove('active');
    uploadProgress.value = 0;
    progressText.textContent = 'Загрузка... 0%';
}

// ==========================================
// ОТПРАВКА ФОРМЫ
// ==========================================

document.getElementById('uploadForm').addEventListener('submit', function (e) {
    const file = fileInput.files[0];
    if (!file) {
        e.preventDefault();
        UIkit.notification({
            message: 'Выберите файл для загрузки',
            status: 'warning',
            timeout: 3000
        });
        return;
    }

    progressBar.classList.add('active');
    uploadBtn.disabled = true;
    uploadBtn.innerHTML = '<span uk-icon="icon: spinner; ratio: 1.2"></span> Загрузка...';

    let progress = 0;
    const interval = setInterval(() => {
        progress += Math.random() * 10;
        if (progress > 90) {
            progress = 90;
            clearInterval(interval);
        }
        uploadProgress.value = progress;
        progressText.textContent = 'Загрузка... ' + Math.round(progress) + '%';
    }, 100);
});

// ==========================================
// ДОПОЛНИТЕЛЬНЫЕ ФУНКЦИИ
// ==========================================

function checkAll() {
    UIkit.notification({
        message: 'Запуск проверки...',
        status: 'primary',
        timeout: 2000
    });

    fetch('/api/cutting-area/validate-all', {method: 'POST'})
        .then(response => {
            if (response.ok) window.location.reload();
            else throw new Error('Ошибка проверки');
        })
        .catch(error => {
            UIkit.notification({
                message: 'Ошибка: ' + error.message,
                status: 'danger',
                timeout: 3000
            });
        });
}

function downloadTemplate() {
    const template = 'plot_number,forestry_name,description,wkt_geometry\n' +
        'Л-12-2024,Пригородное,Участок 12,"POLYGON((30.1 50.2, 30.2 50.3, 30.0 50.1, 30.1 50.2))"\n' +
        'Л-13-2024,Пригородное,Участок 13,"POLYGON((30.3 50.3, 30.4 50.4, 30.2 50.2, 30.3 50.3))"\n';

    const blob = new Blob(['\uFEFF' + template], {type: 'text/csv;charset=utf-8;'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'template_import.csv';
    link.click();
    URL.revokeObjectURL(link.href);

    UIkit.notification({
        message: 'Шаблон скачан!',
        status: 'success',
        timeout: 2000
    });
}


