(function() {
    'use strict';

    angular
        .module('clockinApp')
        .filter('capitalize', capitalize);

    function capitalize() {
        return capitalizeFilter;

        function capitalizeFilter (input) {
            if (input !== null) {
                input = new String(input);
                input = input.toLowerCase();

                return input.substring(0, 1).toUpperCase() + input.substring(1);
            }

            return input;
        }
    }
})();
