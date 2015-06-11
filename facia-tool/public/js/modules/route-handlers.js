import {init, update, differs} from 'modules/vars';
import Collections from 'models/collections/main';
import Config from 'models/config/main';
import Layout from 'models/layout';
import columns from 'models/columns';

class Loader {
    constructor(ModuleClass, enabledWidgets, router, res) {
        let instance = new ModuleClass();
        instance.model.layout = new Layout(router, enabledWidgets);
        this.instance = instance;
        instance.init(res);
    }

    update(res) {
        if (differs(res)) {
            update(res);
            this.instance.update(res);
        }
    }
}

function getLoader (ModuleClass, enabledWidgets) {
    return function (router, res) {
        init(res);

        var module = new Loader(ModuleClass, enabledWidgets, router, res);
        // update(res) has to be called after because there's a vars.setModel inside the ModuleClass
        update(res);
        return module;
    };
}

export default {
    'fronts': getLoader(Collections, [
        columns.frontsTrail,
        columns.frontsTreats,
        columns.latestTrail,
        columns.clipboardTrail
    ]),
    'config': getLoader(Config, [
        columns.clipboardCollection,
        columns.frontsConfig
    ])
};
