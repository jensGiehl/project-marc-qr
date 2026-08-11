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
    const cornerRadius = document.querySelector('#batchCornerRadius');
    const cornerRadiusValue = document.querySelector('#batchCornerRadiusValue');
    const imageCornerRadius = document.querySelector('#batchImageCornerRadius');
    const imageCornerRadiusValue = document.querySelector('#batchImageCornerRadiusValue');

    function nonEmptyLines() {
        return lines.value.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
    }

    function updateLineCounter() {
        const count = nonEmptyLines().length;
        counter.textContent = `${count} / 100`;
        counter.classList.toggle('text-danger', count > 100);
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
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Wird erzeugt …';
        status.textContent = 'Je nach Anzahl kann das einen Moment dauern.';

        const data = new FormData();
        data.append('lines', lines.value);
        data.append('size', document.querySelector('#batchSize').value);
        data.append('foreground', document.querySelector('#batchForeground').value);
        data.append('background', document.querySelector('#batchBackground').value);
        data.append('cornerRadius', cornerRadius.value);
        data.append('imageCornerRadius', imageCornerRadius.value);
        const logo = document.querySelector('#batchLogo').files[0];
        if (logo) data.append('logo', logo);

        try {
            const response = await fetch('/api/qr/batch', { method: 'POST', body: data });
            if (!response.ok) {
                const problem = await response.json().catch(() => ({ message: 'Die QR-Codes konnten nicht erstellt werden.' }));
                throw new Error(problem.message);
            }
            const items = await response.json();
            renderResults(items);
            status.textContent = `${items.length} QR-Code${items.length === 1 ? '' : 's'} erfolgreich erzeugt.`;
        } catch (requestError) {
            showError(requestError.message);
            status.textContent = '';
        } finally {
            button.disabled = false;
            button.textContent = 'QR-Codes erzeugen';
        }
    });

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
            link.download = QrFilename.create(item.input, item.type === 'Webseite');
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
