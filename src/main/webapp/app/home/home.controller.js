(function() {
    'use strict';

    angular
        .module('clockinApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal', 'LoginService', '$state', '$window', '$rootScope'];

    function HomeController ($scope, Principal, LoginService, $state, $window, $rootScope) {
        var vm = this;
		var today = new Date();

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = LoginService.open;
        vm.register = register;
        $scope.$on('authenticationSuccess', function() {
            getAccount();
        });

        getAccount();

        function getAccount() {
            Principal.identity().then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;

				if(vm.account){
					$rootScope.firstName = $window.localStorage.firstName = vm.account.firstName;

					$state.go("clockin-table", {year: today.getFullYear(), month: today.getMonth()});
				}
            });
        }
        function register () {
            $state.go('register');
        }
    }
})();
