import ko from 'knockout';
import _ from 'underscore';
import $ from 'jquery';
import 'jquery-ui/ui/effect';
import 'jquery-ui/ui/effect-size';
import 'jquery-ui/ui/effect-scale';
import Column from 'models/layout/column';
import copiedArticle from 'modules/copied-article';
import * as layoutFromURL from 'utils/layout-from-url';
import updateScrollables from 'utils/update-scrollables';

function columnDataOf (type, columns) {
    return _.find(columns, col => {
        return col.layoutType === type;
    }) || {};
}

export default class Layout {
    constructor(router, widgets) {
        this.CONST = {
            addColumnTransition: 300,
            removeColumnTransition: 200
        };
        this.router = router;

        this.allColumns = widgets;
        this.availableColumns = _.filter(widgets, config => {
            return config.selectable !== false;
        });

        this.configVisible = ko.observable(false);

        this.savedState = {
            columns: ko.observableArray()
        };
        this.currentState = {
            columns: ko.observableArray()
        };

        this.initializeFromLocation();
        this.configVisible.subscribe(this.onConfigVisibilityChange.bind(this));
        this.locationChangeCallback = this.locationChange.bind(this);
        router.on('change', this.locationChangeCallback);
    }

    dispose() {
        this.router.off('change', this.locationChangeCallback);
    }

    locationChange() {
        this.initializeFromLocation();
        this.recomputeAllWidths();
    }

    initializeFromLocation() {
        let layout = layoutFromURL.get(this.router.params);
        this.savedState.columns(_.map(layout, col => this.newConfigInstance(col)));
        this.applyToCurrentState(this.savedState.columns());
    }

    newConfigInstance(config) {
        return new Column(_.extend({}, config, {
            'layout': this
        }, columnDataOf(config.type, this.allColumns)));
    }

    applyToCurrentState(columns) {
        var currentColumns = this.currentState.columns() || [];
        this.currentState.columns(_.map(columns, (column, position) => {
            var current = currentColumns[position] || new Column(column.opts);
            if (column.sameAs(current)) {
                current.setConfig(column.config());
            } else {
                current = column;
            }
            return current;
        }));
    }

    toggleConfigVisible() {
        this.configVisible(!this.configVisible());
    }

    save() {
        this.savedState.columns(this.currentState.columns().slice());
        this.applyToCurrentState(this.savedState.columns());
        this.onColumnChange();
        this.configVisible(false);
    }

    cancel() {
        this.configVisible(false);
        this.currentState.columns(this.savedState.columns().slice());
    }

    onColumnChange() {
        this.router.navigate({
            layout: layoutFromURL.serialize(_.map(this.savedState.columns(), column => column.serializable()))
        });
    }

    addColumn(column) {
        var position = this.currentState.columns.indexOf(column);
        this.currentState.columns.splice(position + 1, 0, this.newConfigInstance({
            type: 'front',
            layout: this
        }));
    }

    removeColumn(column) {
        if (this.currentState.columns().length === 1) {
            return;
        }
        var position = this.currentState.columns.indexOf(column);
        this.currentState.columns.splice(position, 1);
    }

    setType(newType, column) {
        var position = this.currentState.columns.indexOf(column);
        this.currentState.columns.splice(position, 1, this.newConfigInstance({
            type: newType,
            layout: this
        }));
    }

    serializable() {
        return _.map(this.columns(), function (column) {
            return column.serializable();
        });
    }

    onConfigVisibilityChange() {
        copiedArticle.flush();
        this.recomputeAllWidths();
    }

    recomputeAllWidths() {
        _.each(this.currentState.columns(), function (column) {
            column.recomputeWidth();
        });
    }
}

ko.bindingHandlers.slideIn = {
    init: function (element, valueAccessor) {
        var value = ko.unwrap(valueAccessor()),
            $element = $(element);
        $element.css({
            marginLeft: value ? 0 : $(element).outerWidth()
        });
        if (value) {
            $element.show();
        } else {
            $element.hide();
        }
    },
    update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        var value = ko.unwrap(valueAccessor()),
            $element = $(element);
        if (value) {
            $element.show();
        }
        $element.animate({
            marginLeft: value ? 0 : $(element).outerWidth()
        }, {
            complete: function () {
                if (!value) {
                    $element.hide();
                }
                updateScrollables();
                bindingContext.$data.layout.onConfigVisibilityChange();
            }
        });
    }
};
