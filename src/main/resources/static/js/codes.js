(() => {
    'use strict';

    const form = document.querySelector('#codeForm');
    const inputs = [...document.querySelectorAll('.character-input')];
    const lengthInput = document.querySelector('#codeLength');
    const maxDigitsInput = document.querySelector('#maxDigits');
    const countInput = document.querySelector('#codeCount');
    const possibilityOutput = document.querySelector('#possibilityCount');
    const error = document.querySelector('#codeError');
    const tokenInput = document.querySelector('#downloadToken');
    const waitingModal = new bootstrap.Modal(document.querySelector('#waitingModal'));
    let downloadPoll;

    function binomial(n, k) {
        if (k < 0 || k > n) return 0n;
        k = Math.min(k, n - k);
        let result = 1n;
        for (let i = 1; i <= k; i++) result = result * BigInt(n - k + i) / BigInt(i);
        return result;
    }

    function pow(base, exponent) {
        return BigInt(base) ** BigInt(exponent);
    }

    function calculatePossibilities() {
        const selected = inputs.filter(input => input.checked).map(input => input.value);
        const digits = selected.filter(value => /\d/.test(value)).length;
        const letters = selected.length - digits;
        const length = Number.parseInt(lengthInput.value, 10) || 0;
        const maxDigits = Math.min(Number.parseInt(maxDigitsInput.value, 10) || 0, length);
        let total = 0n;
        if (length >= 1) {
            for (let digitCount = 0; digitCount <= maxDigits; digitCount++) {
                const letterCount = length - digitCount;
                if ((digitCount > 0 && digits === 0) || (letterCount > 0 && letters === 0)) continue;
                total += binomial(length, digitCount) * pow(digits, digitCount) * pow(letters, letterCount);
            }
        }
        possibilityOutput.textContent = total.toLocaleString('de-DE');
        possibilityOutput.dataset.raw = total.toString();
        maxDigitsInput.max = Math.max(1, length).toString();
        if (Number(maxDigitsInput.value) > length) maxDigitsInput.value = length.toString();
        countInput.max = (total > 100000n ? 100000n : total).toString();
        validate(false);
    }

    function validate(showMessage = true) {
        const selectedCount = inputs.filter(input => input.checked).length;
        const length = Number(lengthInput.value);
        const maxDigits = Number(maxDigitsInput.value);
        const requested = BigInt(countInput.value || '0');
        const possible = BigInt(possibilityOutput.dataset.raw || '0');
        let message = '';
        if (!selectedCount) message = 'Bitte mindestens ein Zeichen auswählen.';
        else if (length < 1 || length > 32) message = 'Die Codelänge muss zwischen 1 und 32 liegen.';
        else if (maxDigits < 0 || maxDigits > length) message = 'Die maximale Ziffernanzahl darf nicht größer als die Codelänge sein.';
        else if (requested < 1n) message = 'Bitte mindestens einen Code anfordern.';
        else if (requested > 100000n) message = 'Pro Export sind höchstens 100.000 Codes möglich.';
        else if (requested > possible) message = `Mit dieser Auswahl sind nur ${possible.toLocaleString('de-DE')} eindeutige Codes möglich.`;

        error.textContent = message;
        error.classList.toggle('d-none', !message || !showMessage);
        return !message;
    }

    function setSelection(mode) {
        inputs.forEach(input => {
            input.checked = mode === 'all' || (mode === 'recommended' && !['0', '1', 'O'].includes(input.value));
        });
        calculatePossibilities();
    }

    form.addEventListener('submit', event => {
        if (!validate(true)) {
            event.preventDefault();
            error.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }
        const token = `${Date.now()}-${crypto.getRandomValues(new Uint32Array(1))[0]}`;
        tokenInput.value = token;
        waitingModal.show();
        window.clearInterval(downloadPoll);
        downloadPoll = window.setInterval(() => {
            if (document.cookie.split(';').some(cookie => cookie.trim() === `downloadToken=${encodeURIComponent(token)}`)) {
                document.cookie = 'downloadToken=; Max-Age=0; Path=/; SameSite=Lax';
                window.clearInterval(downloadPoll);
                waitingModal.hide();
            }
        }, 300);
        window.setTimeout(() => {
            window.clearInterval(downloadPoll);
            waitingModal.hide();
        }, 60000);
    });

    inputs.forEach(input => input.addEventListener('change', calculatePossibilities));
    [lengthInput, maxDigitsInput, countInput].forEach(input => input.addEventListener('input', calculatePossibilities));
    document.querySelector('#selectRecommended').addEventListener('click', () => setSelection('recommended'));
    document.querySelector('#selectAll').addEventListener('click', () => setSelection('all'));
    document.querySelector('#selectNone').addEventListener('click', () => setSelection('none'));
    calculatePossibilities();
})();
