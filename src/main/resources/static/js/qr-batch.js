(() => {
    'use strict';

    const form = document.querySelector('#batchForm');
    const lines = document.querySelector('#batchLines');
    const counter = document.querySelector('#lineCounter');
    const button = document.querySelector('#generateBatch');
    const status = document.querySelector('#batchStatus');
    const error = document.querySelector('#batchError');
    const resultSection = document.querySelector('#batchResults');
    const resultBody = document.querySelector('#batchResultBody');
    const resultCount = document.querySelector('#resultCount');
    const downloadAll = document.querySelector('#downloadAll');
    const downloadWaitElement = document.querySelector('#downloadWaitModal');
    const downloadWaitModal = bootstrap.Modal.getOrCreateInstance(downloadWaitElement);
    const cornerRadius = document.querySelector('#batchCornerRadius');
    const cornerRadiusValue = document.querySelector('#batchCornerRadiusValue');
    const imageCornerRadius = document.querySelector('#batchImageCornerRadius');
    const imageCornerRadiusValue = document.querySelector('#batchImageCornerRadiusValue');
    let lastBatchRequest;

    function nonEmptyLines() {
        return lines.value.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
    }

    function updateLineCounter() {
        const count = nonEmptyLines().length;
        counter.textContent = `${count} / 100`;
        counter.classList.toggle('text-danger', count > 100);
        if (!button.disabled) button.textContent = `QR-Codes erzeugen (${count})`;
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        error.classList.add('d-none');
        const inputs = nonEmptyLines();
        if (!inputs.length || inputs.length > 100) {
            showError(inputs.length ? 'Bitte höchstens 100 nicht leere Zeilen eingeben.' : 'Bitte mindestens eine nicht leere Zeile eingeben.');
            return;
        }

        button.disabled = true;
        button.innerHTML = `<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Wird erzeugt (${inputs.length}) …`;
        status.textContent = 'Je nach Anzahl kann das einen Moment dauern.';

        const request = captureRequest();

        try {
            const response = await fetch('/api/qr/batch', { method: 'POST', body: createFormData(request) });
            if (!response.ok) {
                const problem = await response.json().catch(() => ({ message: 'Die QR-Codes konnten nicht erstellt werden.' }));
                throw new Error(problem.message);
            }
            const items = await response.json();
            lastBatchRequest = request;
            renderResults(items);
            status.textContent = `${items.length} QR-Code${items.length === 1 ? '' : 's'} erfolgreich erzeugt.`;
        } catch (requestError) {
            showError(requestError.message);
            status.textContent = '';
        } finally {
            button.disabled = false;
            updateLineCounter();
        }
    });

    downloadAll.addEventListener('click', async () => {
        if (!lastBatchRequest) return;

        error.classList.add('d-none');
        downloadAll.disabled = true;
        await showDownloadWaitModal();

        try {
            const response = await fetch('/api/qr/batch/zip', {
                method: 'POST',
                body: createFormData(lastBatchRequest)
            });
            if (!response.ok) {
                const problem = await response.json().catch(() => ({ message: 'Die ZIP-Datei konnte nicht erstellt werden.' }));
                throw new Error(problem.message);
            }
            const archiveUrl = URL.createObjectURL(await response.blob());
            const link = document.createElement('a');
            link.href = archiveUrl;
            link.download = 'qr-codes.zip';
            link.click();
            window.setTimeout(() => URL.revokeObjectURL(archiveUrl), 1_000);
            status.textContent = 'Die ZIP-Datei wurde erfolgreich erstellt.';
        } catch (requestError) {
            showError(requestError.message);
        } finally {
            downloadWaitModal.hide();
            downloadAll.disabled = false;
        }
    });

    function showDownloadWaitModal() {
        if (downloadWaitElement.classList.contains('show')) return Promise.resolve();
        return new Promise(resolve => {
            downloadWaitElement.addEventListener('shown.bs.modal', resolve, { once: true });
            downloadWaitModal.show();
        });
    }

    function captureRequest() {
        return {
            lines: lines.value,
            size: document.querySelector('#batchSize').value,
            foreground: document.querySelector('#batchForeground').value,
            background: document.querySelector('#batchBackground').value,
            cornerRadius: cornerRadius.value,
            imageCornerRadius: imageCornerRadius.value,
            logo: document.querySelector('#batchLogo').files[0]
        };
    }

    function createFormData(request) {
        const data = new FormData();
        data.append('lines', request.lines);
        data.append('size', request.size);
        data.append('foreground', request.foreground);
        data.append('background', request.background);
        data.append('cornerRadius', request.cornerRadius);
        data.append('imageCornerRadius', request.imageCornerRadius);
        if (request.logo) data.append('logo', request.logo);
        return data;
    }

    function renderResults(items) {
        resultBody.replaceChildren();
        items.forEach(item => {
            const row = document.createElement('tr');

            const imageCell = document.createElement('td');
            const image = document.createElement('img');
            image.className = 'batch-qr';
            image.src = `data:image/png;base64,${item.imageBase64}`;
            image.alt = `QR-Code für ${item.input}`;
            imageCell.append(image);

            const contentCell = document.createElement('td');
            const typeBadge = document.createElement('span');
            typeBadge.className = 'badge rounded-pill text-bg-light mb-2';
            typeBadge.textContent = item.type;
            const text = document.createElement('div');
            text.className = 'fw-semibold payload-preview';
            text.textContent = item.input;
            contentCell.append(typeBadge, text);

            const downloadCell = document.createElement('td');
            downloadCell.className = 'text-end';
            const link = document.createElement('a');
            link.className = 'btn btn-outline-primary btn-sm';
            link.href = image.src;
            link.download = item.filename;
            link.textContent = 'PNG';
            downloadCell.append(link);

            row.append(imageCell, contentCell, downloadCell);
            resultBody.append(row);
        });
        resultCount.textContent = `${items.length} Ergebnis${items.length === 1 ? '' : 'se'}`;
        resultSection.classList.remove('d-none');
        resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function showError(message) {
        error.textContent = message;
        error.classList.remove('d-none');
    }

    lines.addEventListener('input', updateLineCounter);
    cornerRadius.addEventListener('input', () => cornerRadiusValue.value = `${cornerRadius.value} %`);
    imageCornerRadius.addEventListener('input', () => imageCornerRadiusValue.value = `${imageCornerRadius.value} %`);
    updateLineCounter();
})();
