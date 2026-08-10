(() => {
    'use strict';

    const form = document.querySelector('#qrForm');
    const type = document.querySelector('#contentType');
    const value = document.querySelector('#contentValue');
    const message = document.querySelector('#messageValue');
    const messageGroup = document.querySelector('#messageGroup');
    const label = document.querySelector('#contentLabel');
    const hint = document.querySelector('#contentHint');
    const messageLabel = document.querySelector('#messageLabel');
    const logo = document.querySelector('#logo');
    const removeLogo = document.querySelector('#removeLogo');
    const preview = document.querySelector('#qrPreview');
    const spinner = document.querySelector('#qrSpinner');
    const error = document.querySelector('#qrError');
    const counter = document.querySelector('#capacityCounter');
    const download = document.querySelector('#downloadQr');
    let currentUrl;
    let controller;
    let debounceTimer;

    const typeConfig = {
        text: { label: 'Text', placeholder: 'Was soll im QR-Code stehen?', hint: 'Freier Text, Zahlen oder kurze Hinweise.' },
        url: { label: 'Webadresse', placeholder: 'https://example.org', hint: 'Fehlt das Protokoll, ergänzen wir automatisch https://.' },
        email: { label: 'E-Mail-Adresse', placeholder: 'hallo@example.org', hint: 'Öffnet beim Scannen das E-Mail-Programm.', message: 'Betreff (optional)' },
        phone: { label: 'Telefonnummer', placeholder: '+49 123 456789', hint: 'Öffnet beim Scannen die Telefon-App.' },
        sms: { label: 'Telefonnummer', placeholder: '+49 123 456789', hint: 'Öffnet beim Scannen eine vorbereitete SMS.', message: 'Nachricht (optional)' }
    };

    function updateType() {
        const config = typeConfig[type.value];
        label.textContent = config.label;
        value.placeholder = config.placeholder;
        hint.textContent = config.hint;
        messageGroup.classList.toggle('d-none', !config.message);
        if (config.message) messageLabel.textContent = config.message;
        scheduleRender();
    }

    function buildContent() {
        const raw = value.value.trim();
        if (!raw) return '';
        switch (type.value) {
            case 'url':
                return /^[a-z][a-z\d+.-]*:\/\//i.test(raw) ? raw : `https://${raw}`;
            case 'email': {
                const subject = message.value.trim();
                return `mailto:${raw}${subject ? `?subject=${encodeURIComponent(subject)}` : ''}`;
            }
            case 'phone':
                return `tel:${raw.replace(/\s/g, '')}`;
            case 'sms':
                return `SMSTO:${raw.replace(/\s/g, '')}:${message.value.trim()}`;
            default:
                return raw;
        }
    }

    function capacity(content) {
        const withLogo = logo.files.length > 0;
        if (/^[0-9]*$/.test(content)) return [content.length, withLogo ? 3057 : 5596, 'Ziffern'];
        if (/^[0-9A-Z $%*+.\/:-]*$/.test(content)) return [content.length, withLogo ? 1852 : 3391, 'Zeichen'];
        return [new TextEncoder().encode(content).length, withLogo ? 1273 : 2331, 'UTF-8-Bytes'];
    }

    function updateCounter(content) {
        const [used, maximum, unit] = capacity(content);
        counter.textContent = `${used.toLocaleString('de-DE')} / ${maximum.toLocaleString('de-DE')} ${unit}`;
        counter.classList.toggle('is-over', used > maximum);
        return used <= maximum;
    }

    function scheduleRender() {
        window.clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(render, 320);
    }

    async function render() {
        const content = buildContent();
        error.classList.add('d-none');
        if (!content || !updateCounter(content)) {
            preview.classList.add('d-none');
            spinner.classList.add('d-none');
            setDownload(null);
            if (content) showError('Der Inhalt ist für diese Einstellungen zu lang.');
            return;
        }

        controller?.abort();
        controller = new AbortController();
        spinner.classList.remove('d-none');
        const data = new FormData();
        data.append('content', content);
        data.append('size', document.querySelector('#qrSize').value);
        data.append('foreground', document.querySelector('#foreground').value);
        data.append('background', document.querySelector('#background').value);
        if (logo.files[0]) data.append('logo', logo.files[0]);

        try {
            const response = await fetch('/api/qr', { method: 'POST', body: data, signal: controller.signal });
            if (!response.ok) {
                const problem = await response.json().catch(() => ({ message: 'Der QR-Code konnte nicht erstellt werden.' }));
                throw new Error(problem.message);
            }
            const blob = await response.blob();
            const objectUrl = URL.createObjectURL(blob);
            if (currentUrl) URL.revokeObjectURL(currentUrl);
            currentUrl = objectUrl;
            preview.src = objectUrl;
            preview.classList.remove('d-none');
            setDownload(objectUrl);
        } catch (requestError) {
            if (requestError.name !== 'AbortError') {
                preview.classList.add('d-none');
                setDownload(null);
                showError(requestError.message);
            }
        } finally {
            spinner.classList.add('d-none');
        }
    }

    function showError(messageText) {
        error.textContent = messageText;
        error.classList.remove('d-none');
    }

    function setDownload(url) {
        if (url) {
            download.href = url;
            download.classList.remove('disabled');
            download.setAttribute('aria-disabled', 'false');
        } else {
            download.removeAttribute('href');
            download.classList.add('disabled');
            download.setAttribute('aria-disabled', 'true');
        }
    }

    form.addEventListener('input', scheduleRender);
    form.addEventListener('change', scheduleRender);
    type.addEventListener('change', updateType);
    logo.addEventListener('change', () => removeLogo.classList.toggle('d-none', !logo.files.length));
    removeLogo.addEventListener('click', () => {
        logo.value = '';
        removeLogo.classList.add('d-none');
        scheduleRender();
    });
    window.addEventListener('beforeunload', () => currentUrl && URL.revokeObjectURL(currentUrl));

    updateType();
})();
