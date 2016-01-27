/*
 *  Waltz
 * Copyright (c) David Watkins. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 *
 */

import _ from 'lodash';
import angular from 'angular';

import { buildHierarchies } from '../common';
import { talliesById } from '../common/tally-utils';


function controller(capabilities, appCapabilityStore, svgStore, $state) {
    const vm = this;

    vm.capabilityHierarchy = buildHierarchies(capabilities);

    appCapabilityStore
        .countByCapabilityId()
        .then(tallies => vm.tallies = talliesById(tallies));


    svgStore.findByKind('CAPABILITY').then(xs => vm.diagrams = xs);

    vm.blockProcessor = block => {
        block.parent.onclick = () => $state.go('main.capabilities.view', { id: block.value });
        angular.element(block.parent).addClass('clickable');
    };
}

controller.$inject = ['capabilities', 'AppCapabilityStore', 'SvgDiagramStore', '$state'];


export default {
    template: require('./list-view.html'),
    controller,
    controllerAs: 'ctrl'
};