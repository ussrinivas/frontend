import {CONST} from 'modules/vars';
import _ from 'underscore';

export default function countFronts (fronts, priority) {
    priority = priority || CONST.defaultPriority;

    return {
        count: _.countBy(fronts, function (front) {
            return front.props.priority() || CONST.defaultPriority;
        })[priority],
        max: CONST.maxFronts[priority] || Infinity
    };
}
