import ko from 'knockout';
import _ from 'underscore';
import * as globalListeners from 'utils/global-listeners';

function isNarrow (column) {
    var percentage = parseInt(column.style.width(), 10),
        width = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);

    return width * percentage / 100 <= 550;
}

export default class Column {
    constructor(opts) {
        this.opts = opts;

        this.layout = opts.layout;
        this.component = {
            name: opts.widget,
            params: _.extend({
                column: this
            }, opts.params)
        };
        this.config = ko.observable(opts.config);

        this.style = {
            width: function () {
                return 100 / opts.layout.currentState.columns().length + 'vw';
            },
            left: function (data) {
                return 100 / opts.layout.currentState.columns().length * opts.layout.currentState.columns.indexOf(data) + 'vw';
            },
            isNarrow: ko.observable()
        };
        _.delay(() => this.recomputeWidth(), 25);

        _.forEach(this.layout.allColumns, function (col) {
            this['is' + col.layoutType] = ko.pureComputed(() => this.opts.type === col.layoutType);
        }, this);

        this.onResizeCallback = _.debounce(() => this.recomputeWidth(), 25);
        globalListeners.on('resize', this.onResizeCallback);
    }

    dispose() {
        // TODO this might be called automatically by ko
        globalListeners.off('resize', this.onResizeCallback);
    }

    serializable() {
        var serialized = {
            type: this.opts.type
        };
        if (this.config()) {
            serialized.config = this.config();
        }
        return serialized;
    }

    recomputeWidth() {
        this.style.isNarrow(isNarrow(this));
    }

    setConfig(newConfig) {
        if (!_.isEqual(newConfig, this.config())) {
            this.config(newConfig);
        }
        this.layout.onColumnChange();
    }

    sameAs(column) {
        if (!column) {
            return false;
        }
        var opts = this.opts;
        return _.isEqual(opts.widget, column.opts.widget) && _.isEqual(opts.params, column.opts.params);
    }
}
