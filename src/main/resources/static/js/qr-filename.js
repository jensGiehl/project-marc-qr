(() => {
    'use strict';

    const reservedNames = /^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)/i;

    function domain(input) {
        try {
            const address = /^[a-z][a-z\d+.-]*:\/\//i.test(input) ? input : `https://${input}`;
            return new URL(address).hostname;
        } catch {
            return input;
        }
    }

    function sanitize(input) {
        const cleaned = input
            .normalize('NFKC')
            .replace(/[<>:"/\\|?*\u0000-\u001f\u007f]/g, '')
            .replace(/\s+/g, ' ')
            .trim()
            .replace(/[. ]+$/g, '')
            .slice(0, 120)
            .replace(/[. ]+$/g, '');
        if (!cleaned) return 'qr-code';
        return reservedNames.test(cleaned) ? `qr-${cleaned}` : cleaned;
    }

    window.QrFilename = {
        create(input, website = false) {
            return `${sanitize(website ? domain(input.trim()) : input)}.png`;
        }
    };
})();
