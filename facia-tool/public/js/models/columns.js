export default {
    // TODO checked???
    'frontsTrail': {
        title: 'Front',
        layoutType: 'front',
        widget: 'fronts-widget'
    },
    'frontsTreats': {
        title: 'Treats',
        layoutType: 'treats',
        widget: 'fronts-widget',
        params: {
            mode: 'treats'
        },
        selectable: false
    },
    'latestTrail': {
        title: 'Latest',
        layoutType: 'latest',
        widget: 'latest-widget'
    },
    'clipboardTrail': {
        title: 'Clipboard',
        layoutType: 'clipboard',
        // TODO
        widget: 'fronts-standalone-clipboard'
    },
    'clipboardCollection': {
        title: 'Clipboard',
        // TODO Are these clipboard different?
        layoutType: 'clipboard',
        widget: '',
        params: ''
    },
    'frontsConfig': {
        title: 'Fronts',
        layoutType: 'config',
        widget: '',
        params: ''
    }
};
