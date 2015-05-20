import ko from 'knockout';
import {CONST} from 'modules/vars';
import asObservableProps from 'utils/as-observable-props';
import firstById from 'utils/find-first-by-id';
import populate from 'utils/populate-observables';
import remove from 'utils/remove-by-id';
import count from 'utils/front-count';

describe('utils/as-observable-props', function () {
    it('generates observables', function () {
        var names = ['one', 'two', 'three'],
            props = asObservableProps(names);

        expect(Object.keys(props)).toEqual(names);
        names.forEach(function (name) {
            props[name]('test value: ' + name);
            expect(props[name]()).toBe('test value: ' + name);
        });
    });
});

describe('utils/find-first-by-id', function () {
    it('find by id', function () {
        var array = ko.observableArray();
        expect(firstById(array, 'banana')).toBe(null);

        array([{
            id: 'some',
            pos: 1
        }, {
            id: 'banana',
            pos: 2
        }]);
        expect(firstById(array, 'banana').pos).toBe(2);

        array([{
            id: ko.observable('banana'),
            pos: 1
        }, {
            id: 'banana',
            pos: 2
        }]);
        expect(firstById(array, 'banana').pos).toEqual(1);
    });
});

describe('utils/populate-observables', function () {
    it('populate observables', function () {
        var target = {
            one: ko.observable(),
            two: ko.observable(2),
            three: ko.observable('three'),
            four: ko.observable()
        },
        value = {

        };

        function toObj(host) {
            var obj = {};
            for (var key in host) {
                obj[key] = host[key]();
            }
            return obj;
        }

        expect(populate(target, null)).toBeUndefined();
        expect(populate(null, value)).toBeUndefined();

        populate(target, {});
        expect(toObj(target)).toEqual({
            one: undefined,
            two: 2,
            three: 'three',
            four: undefined
        });

        populate(target, {
            one: 'string',
            two: 'number',
            three: null,
            four: []
        });
        expect(toObj(target)).toEqual({
            one: 'string',
            two: 2,
            three: 'three',
            four: []
        });
    });
});

describe('utils/remove-by-id', function () {
    it('remove elements', function () {
        var array = ko.observableArray([{
            id: 'banana',
            pos: 1
        }, {
            id: ko.observable('banana'),
            pos: 2
        }, {
            id: 'apple',
            pos: 3
        }]);

        var removed = remove(array, 'banana');
        expect(array()).toEqual([{
            id: 'apple',
            pos: 3
        }]);
        expect(removed.pos).toBe(1);
    });
});

describe('utils/front-count', function () {
    var Front = function (priority) {
        this.props = {
            priority: ko.observable(priority)
        };
    },
    fronts = [
        new Front(),
        new Front(),
        new Front('commercial'),
        new Front('editorial'),
        new Front('whatever'),
        new Front('whatever')
    ];

    it('counts fronts without priority', function () {
        var result = count(fronts, 'editorial');
        expect(result).toEqual({
            count: 3,
            max: CONST.maxFronts.editorial
        });
    });

    it('defaults to editorial priority', function () {
        var result = count(fronts);
        expect(result).toEqual({
            count: 3,
            max: CONST.maxFronts.editorial
        });
    });

    it('counts fronts with any priority', function () {
        var result = count(fronts, 'commercial');
        expect(result).toEqual({
            count: 1,
            max: CONST.maxFronts.commercial
        });
    });

    it('counts fronts with no limits', function () {
        var result = count(fronts, 'whatever');
        expect(result).toEqual({
            count: 2,
            max: Infinity
        });
    });
});
