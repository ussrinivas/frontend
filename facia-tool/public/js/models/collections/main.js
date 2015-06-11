import _ from 'underscore';
import ko from 'knockout';
import View from 'models/view';
import * as vars from 'modules/vars';
import mediator from 'utils/mediator';
import * as sparklines from 'utils/sparklines';
import parseQueryParams from 'utils/parse-query-params';
import ListManager from 'modules/list-manager';
import modalDialog from 'modules/modal-dialog';
import newItems from 'models/collections/new-items';
import copiedArticle from 'modules/copied-article';

export default class Fronts extends View {
    constructor() {
        super();
        // TODO change this in the list manager
        this.listManager = new ListManager(newItems);

        this.setModel({
            title: ko.observable('fronts'),
            alert: ko.observable(),
            modalDialog: modalDialog,
            switches: ko.observable(),
            fronts: ko.observableArray(),
            isPasteActive: ko.observable(false),
            isSparklinesEnabled: ko.pureComputed(function () {
                return sparklines.isEnabled();
            }),
            // TODO see if we can put this in the view
            chooseLayout: function () {
                this.layout.toggleConfigVisible();
            },
            saveLayout: function () {
                this.layout.save();
            },
            cancelLayout: function () {
                this.layout.cancel();
            },
            pressLiveFront: function () {
                this.model.clearAlerts();
                mediator.emit('presser:live');
            },
            clearAlerts: function() {
                this.model.alert(false);
                mediator.emit('alert:dismiss');
            }
        });

        mediator.on('presser:stale', (message) => {
            this.model.alert(message);
        });
        this.onCopiedArticleChangeCallback = this.onCopiedArticleChange.bind(this);
        copiedArticle.on('change', this.onCopiedArticleChangeCallback);
    }

    dispose() {
        this.listManager.dispose();
        mediator.removeEvent('presser:stale');
        copiedArticle.off('change', this.onCopiedArticleChangeCallback);
        super.dispose();
    }

    update(res) {
        super.update(res);
        var fronts;

        var frontInURL = parseQueryParams().front;
        fronts = frontInURL === 'testcard' ? ['testcard'] :
            _.chain(res.config.fronts)
            .map(function(front, path) {
                return front.priority === vars.priority ? path : undefined;
            })
            .without(undefined)
            .without('testcard')
            .difference(vars.CONST.askForConfirmation)
            .sortBy(function(path) { return path; })
            .value();

        if (!_.isEqual(this.model.fronts(), fronts)) {
            this.model.fronts(fronts);
        }
    }

    onCopiedArticleChange(hasArticle) {
        this.model.isPasteActive(hasArticle);
    }
}
