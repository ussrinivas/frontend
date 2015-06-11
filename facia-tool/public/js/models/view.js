import 'models/common-handlers';
import ko from 'knockout';
import EventEmitter from 'EventEmitter';
import * as vars from 'modules/vars';
import droppable from 'modules/droppable';
import copiedArticle from 'modules/copied-article';
import * as globalListeners from 'utils/global-listeners';
import updateScrollables from 'utils/update-scrollables';
import * as widgets from 'models/widgets';

export default class View extends EventEmitter {
    constructor() {
        super();
        // Do all the common logic like creating a layout and instantiating some classes
        // register all the widgets that can be used
    }

    // store the model
    setModel(model) {
        vars.setModel(model);
        this.model = model;
        globalListeners.off('resize', updateScrollables);
    }
    // called when the configuration is ready
    init(res) {
        this.update(res);
        droppable.init();
        copiedArticle.flush();
        widgets.register();
        ko.applyBindings(this.model);

        updateScrollables();
        globalListeners.on('resize', updateScrollables);
    }
    // called when the configuration changes
    update() {}
    // emit the event 'config_needs_update' when the configuration should be fetched again
}
