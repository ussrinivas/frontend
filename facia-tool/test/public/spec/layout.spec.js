import ko from 'knockout';
import $ from 'jquery';
import _ from 'underscore';
import Layout from 'models/layout';
import Router from 'modules/router';
import verticalLayout from 'views/templates/vertical_layout.scala.html!text';
import fakePushState from 'test/utils/push-state';

describe('Layout', function () {
    var CONST_TRANSITION = 10;
    beforeEach(function (done) {
        document.body.innerHTML += '<div id="_test_container_layout">' +
            '<span class="save-layout" data-bind="click: layout.save.bind(layout)">Save</span>' +
            '<span class="cancel-layout" data-bind="click: layout.cancel.bind(layout)">Cancel</span>' +
            verticalLayout +
        '</div>';
        this.container = document.getElementById('_test_container_layout');

        var handlers = {
            fronts: function () {},
            latest: function () {}
        },
        location = {
            pathname: '/',
            search: ''
        },
        history = {
            pushState: function () {}
        };

        spyOn(handlers, 'fronts');
        spyOn(handlers, 'latest');
        spyOn(history, 'pushState').and.callFake(fakePushState.bind(location));
        this.router = new Router(handlers, location, history);
        this.widget = [{
            title: 'Front',
            layoutType: 'front',
            widget: 'mock-front-widget'
        }, {
            title: 'Latest',
            layoutType: 'latest',
            widget: 'mock-latest-widget'
        }];
        this.layout = new Layout(this.router, this.widget);
        this.layout.CONST.addColumnTransition = CONST_TRANSITION;
        this.layout.CONST.removeColumnTransition = CONST_TRANSITION;

        ko.applyBindings({
            layout: this.layout
        }, this.container);

        setTimeout(done, 10);
    });
    afterEach(function () {
        ko.cleanNode(this.container);
        this.container.parentNode.removeChild(this.container);
    });
    function click (selector) {
        return new Promise(resolve => {
            $(selector).click();
            setTimeout(resolve, CONST_TRANSITION + 10);
        });
    }
    function navigateTo (router, search) {
        return new Promise(resolve => {
            router.location.search = search;
            router.onpopstate();
            setTimeout(resolve, 10);
        });
    }
    function columnsInDOM () {
        return _.map($('.mock-widget'), widget => {
            return _.filter(widget.classList, className => className !== 'mock-widget')[0];
        });
    }

    it('changes the workspace', function (done) {
        var layout = this.layout;
        expect(layout.configVisible()).toBe(false);
        expect(layout.configVisible()).toBe(false);
        expect($('.config-pane', this.container).is(':visible')).toBe(false);
        expect(columnsInDOM()).toEqual(['latest', 'front']);

        layout.toggleConfigVisible();
        expect(layout.configVisible()).toBe(true);
        expect($('.config-pane', this.container).is(':visible')).toBe(true);

        var saved = layout.savedState.columns();
        var current = layout.currentState.columns();
        expect(saved.length).toBe(2);
        expect(current.length).toBe(2);

        // Add an extra column in the middle
        click('.fa-plus-circle:nth(0)')
        .then(() => {
            expect(layout.savedState.columns()).toBe(saved);
            expect(layout.currentState.columns().length).toBe(3);
            expect($('.config-pane').length).toBe(3);
            expect(columnsInDOM()).toEqual(['latest', 'front', 'front']);

            // Cancel the workspace change
            return click('.cancel-layout');
        })
        .then(() => {
            expect(layout.savedState.columns()).toBe(saved);
            expect(layout.currentState.columns().length).toBe(2);
            expect($('.config-pane').length).toBe(2);
            expect(columnsInDOM()).toEqual(['latest', 'front']);

            // Add another column
            return click('.fa-plus-circle:nth(1)');
        })
        .then(() => {
            expect(layout.savedState.columns()).toBe(saved);
            expect(layout.currentState.columns().length).toBe(3);
            expect($('.config-pane').length).toBe(3);
            expect(columnsInDOM()).toEqual(['latest', 'front', 'front']);

            // Change the type of a column
            return click('.config-pane:nth(2) .checkbox-latest');
        })
        .then(() => {
            expect(columnsInDOM()).toEqual(['latest', 'front', 'latest']);

            return click('.save-layout');
        })
        .then(() => {
            expect(this.router.location.search).toBe('?layout=latest,front,latest');

            return layout.toggleConfigVisible();
        })
        .then(() => {
            expect(columnsInDOM()).toEqual(['latest', 'front', 'latest']);

            // Navigate back to the previous layout
            return navigateTo(this.router, '?layout=front:banana,latest');
        })
        .then(() => {
            expect(columnsInDOM()).toEqual(['front', 'latest']);
            expect($('.mock-widget.front').text()).toBe('banana');

            layout.currentState.columns()[0].setConfig('apple');
        })
        .then(() => {
            expect(this.router.location.search).toBe('?layout=front:apple,latest');

            return click('.fa-minus-circle:nth(1)');
        })
        .then(() => {
            expect(layout.savedState.columns().length).toBe(2);
            expect(layout.currentState.columns().length).toBe(1);
            expect($('.config-pane').length).toBe(1);
            expect(columnsInDOM()).toEqual(['front']);
            expect($('.mock-widget.front').text()).toBe('apple');
        })
        .then(done);
    });
});

ko.components.register('mock-front-widget', {
    viewModel: {
        createViewModel: (params) => params
    },
    template: '<div class="mock-widget front" data-bind="text: column.config"></div>'
});
ko.components.register('mock-latest-widget', {
    viewModel: {
        createViewModel: (params) => params
    },
    template: '<div class="mock-widget latest"></div>'
});
