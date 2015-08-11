describe('Dataset list controller', function () {
    'use strict';

    var createController, scope;
    var datasets = [
        {id: 'ec4834d9bc2af8', name: 'Customers (50 lines)'},
        {id: 'ab45f893d8e923', name: 'Us states'},
        {id: 'cf98d83dcb9437', name: 'Customers (1K lines)'}
    ];
    var refreshedDatasets = [
        {id: 'ec4834d9bc2af8', name: 'Customers (50 lines)'},
        {id: 'ab45f893d8e923', name: 'Us states'}
    ];

    beforeEach(module('data-prep.dataset-list'));

    beforeEach(inject(function ($rootScope, $controller, $q, $state, DatasetService, PlaygroundService, MessageService) {
        var datasetsValues = [datasets, refreshedDatasets];
        scope = $rootScope.$new();

        createController = function () {
            var ctrl = $controller('DatasetListCtrl', {
                $scope: scope
            });
            return ctrl;
        };

        spyOn(DatasetService, 'processCertification').and.returnValue($q.when(true));
        spyOn(DatasetService, 'getDatasets').and.callFake(function () {
            return $q.when(datasetsValues.shift());
        });

        spyOn(PlaygroundService, 'initPlayground').and.returnValue($q.when(true));
        spyOn(PlaygroundService, 'show').and.callThrough();
        spyOn(MessageService, 'error').and.returnValue(null);
        spyOn($state, 'go').and.returnValue(null);
    }));

    afterEach(inject(function($stateParams, $window) {
        $stateParams.datasetid = null;

        $window.localStorage.removeItem('dataprep.dataset.sort');
        $window.localStorage.removeItem('dataprep.dataset.sortOrder');
    }));

    it('should get dataset on creation', inject(function (DatasetService) {
        //when
        createController();
        scope.$digest();

        //then
        expect(DatasetService.getDatasets).toHaveBeenCalled();
    }));

    describe('dataset in query params load', function() {
        it('should init playground with the provided datasetId from url', inject(function ($stateParams, PlaygroundService) {
            //given
            $stateParams.datasetid = 'ab45f893d8e923';

            //when
            createController();
            scope.$digest();

            //then
            expect(PlaygroundService.initPlayground).toHaveBeenCalledWith(datasets[1]);
            expect(PlaygroundService.show).toHaveBeenCalled();
        }));

        it('should show error message when dataset id is not in users dataset', inject(function ($stateParams, PlaygroundService, MessageService) {
            //given
            $stateParams.datasetid = 'azerty';

            //when
            createController();
            scope.$digest();

            //then
            expect(PlaygroundService.initPlayground).not.toHaveBeenCalled();
            expect(PlaygroundService.show).not.toHaveBeenCalled();
            expect(MessageService.error).toHaveBeenCalledWith('PLAYGROUND_FILE_NOT_FOUND_TITLE', 'PLAYGROUND_FILE_NOT_FOUND', {type: 'dataset'});
        }));
    });

    describe('sort parameters', function() {

        it('should init default sort parameters', inject(function ($window) {
            //given
            $window.localStorage.removeItem('dataprep.dataset.sort');
            $window.localStorage.removeItem('dataprep.dataset.sortOrder');

            //when
            var ctrl = createController();

            //then
            expect(ctrl.sortSelected).toEqual({id: 'date', name: 'DATE_SORT'});
            expect(ctrl.sortOrderSelected).toEqual({id: 'desc', name: 'DESC_ORDER'});
        }));

        it('should init sort parameters from localStorage', inject(function ($window) {
            //given
            $window.localStorage.setItem('dataprep.dataset.sort', 'name');
            $window.localStorage.setItem('dataprep.dataset.sortOrder', 'asc');

            //when
            var ctrl = createController();

            //then
            expect(ctrl.sortSelected).toEqual({id: 'name', name: 'NAME_SORT'});
            expect(ctrl.sortOrderSelected).toEqual({id: 'asc', name: 'ASC_ORDER'});
        }));

        describe('with dataset refresh success', function() {
            beforeEach(inject(function ($q, DatasetService) {
                spyOn(DatasetService, 'refreshDatasets').and.returnValue($q.when(true));
            }));

            it('should refresh dataset when sort is changed without localstorage', inject(function ($window, DatasetService) {
                //given
                $window.localStorage.removeItem('dataprep.dataset.sort');
                $window.localStorage.removeItem('dataprep.dataset.sortOrder');

                //given
                var ctrl = createController();
                var newSort =  {id: 'name', name: 'NAME_SORT'};

                //when
                ctrl.updateSortBy(newSort);

                //then
                expect(DatasetService.refreshDatasets).toHaveBeenCalledWith('name','desc');
            }));

            it('should refresh dataset when order is changed', inject(function ($window, DatasetService) {
                //given
                var ctrl = createController();
                var newSortOrder =  {id: 'asc', name: 'ASC_ORDER'};

                //when
                ctrl.updateSortOrder(newSortOrder);

                //then
                expect(DatasetService.refreshDatasets).toHaveBeenCalledWith('date','asc');
            }));

            it('should refresh dataset when sort is changed', inject(function (DatasetService) {
                //given
                var ctrl = createController();
                var newSort =  {id: 'name', name: 'NAME_SORT'};

                //when
                ctrl.updateSortBy(newSort);

                //then
                expect(DatasetService.refreshDatasets).toHaveBeenCalledWith('name','desc');
            }));

            it('should not refresh dataset when requested sort is already the selected one', inject(function (DatasetService) {
                //given
                var ctrl = createController();
                var newSort =  {id: 'name', name: 'NAME_SORT'};

                //when
                ctrl.updateSortBy(newSort);
                ctrl.updateSortBy(newSort);

                //then
                expect(DatasetService.refreshDatasets.calls.count()).toBe(1);
            }));

            it('should not refresh dataset when requested sort is already the selected one', inject(function (DatasetService) {
                //given
                var ctrl = createController();
                var newSortOrder =  {id: 'desc', name: 'ASC_ORDER'};

                //when
                ctrl.updateSortOrder(newSortOrder);
                ctrl.updateSortOrder(newSortOrder);

                //then
                expect(DatasetService.refreshDatasets.calls.count()).toBe(1);
            }));

            it('should save sort order parameter in localStorage', inject(function ($window) {
                //given
                var ctrl = createController();
                var newSortOrder =  {id: 'asc', name: 'ASC_ORDER'};

                expect($window.localStorage.getItem('dataprep.dataset.sortOrder')).toBeFalsy();

                //when
                ctrl.updateSortOrder(newSortOrder);
                scope.$digest();

                //then
                expect($window.localStorage.getItem('dataprep.dataset.sortOrder')).toBe('asc');
            }));

            it('should save sort parameter in localStorage', inject(function ($window) {
                //given
                var ctrl = createController();
                var newSort =  {id: 'name', name: 'NAME_SORT'};

                //when
                ctrl.updateSortBy(newSort);
                scope.$digest();

                //then
                expect($window.localStorage.getItem('dataprep.dataset.sort')).toBe('name');
            }));
        });

        describe('with dataset refresh failure', function() {
            beforeEach(inject(function ($q, DatasetService) {
                spyOn(DatasetService, 'refreshDatasets').and.returnValue($q.reject(false));
            }));

            it('should NOT save sort order parameter in localStorage', inject(function ($window) {
                //given
                var ctrl = createController();
                var newSortOrder =  {id: 'asc', name: 'ASC_ORDER'};

                expect($window.localStorage.getItem('dataprep.dataset.sortOrder')).toBeFalsy();

                //when
                ctrl.updateSortOrder(newSortOrder);
                scope.$digest();

                //then
                expect($window.localStorage.getItem('dataprep.dataset.sortOrder')).toBeFalsy();
            }));

            it('should NOT save sort parameter in localStorage', inject(function ($window) {
                //given
                var ctrl = createController();
                var newSort =  {id: 'name', name: 'NAME_SORT'};

                //when
                ctrl.updateSortBy(newSort);
                scope.$digest();

                //then
                expect($window.localStorage.getItem('dataprep.dataset.sort')).toBeFalsy();
            }));

            it('should set the old order parameter', function () {
                //given
                var ctrl = createController();
                var newSortOrder =  {id: 'asc', name: 'ASC_ORDER'};

                expect(ctrl.sortOrderSelected.id).toBe('desc');

                //when
                ctrl.updateSortOrder(newSortOrder);
                expect(ctrl.sortOrderSelected.id).toBe('asc');
                scope.$digest();

                //then
                expect(ctrl.sortOrderSelected.id).toBe('desc');
            });

            it('should NOT set the old sort parameter', function () {
                //given
                var ctrl = createController();
                var newSort =  {id: 'name', name: 'NAME_SORT'};

                expect(ctrl.sortSelected.id).toBe('date');

                //when
                ctrl.updateSortBy(newSort);
                expect(ctrl.sortSelected.id).toBe('name');
                scope.$digest();

                //then
                expect(ctrl.sortSelected.id).toBe('date');
            });
        });
    });

    describe('already created', function () {
        var ctrl;

        beforeEach(inject(function ($rootScope, $q, MessageService, DatasetService, DatasetSheetPreviewService) {
            ctrl = createController();
            scope.$digest();

            spyOn(DatasetService, 'refreshDatasets').and.returnValue($q.when(true));
            spyOn(DatasetService, 'delete').and.returnValue($q.when(true));
            spyOn(MessageService, 'success').and.callThrough();
            spyOn(DatasetSheetPreviewService, 'loadPreview').and.returnValue($q.when(true));
            spyOn(DatasetSheetPreviewService, 'display').and.returnValue($q.when(true));
            spyOn(DatasetService, 'toggleFavorite').and.returnValue($q.when(true));
        }));

        it('should delete dataset and show toast', inject(function ($q, MessageService, DatasetService, TalendConfirmService) {
            //given
            var dataset = datasets[0];
            spyOn(TalendConfirmService, 'confirm').and.returnValue($q.when(true));

            //when
            ctrl.delete(dataset);
            scope.$digest();

            //then
            expect(TalendConfirmService.confirm).toHaveBeenCalledWith({disableEnter: true}, [ 'DELETE_PERMANENTLY', 'NO_UNDONE_CONFIRM' ], {type: 'dataset', name: 'Customers (50 lines)' });
            expect(DatasetService.delete).toHaveBeenCalledWith(dataset);
            expect(MessageService.success).toHaveBeenCalledWith('REMOVE_SUCCESS_TITLE', 'REMOVE_SUCCESS', {type: 'dataset', name: 'Customers (50 lines)'});
        }));

        it('should bind datasets getter to DatasetService.datasetsList()', inject(function (DatasetService) {
            //given
            spyOn(DatasetService, 'datasetsList').and.returnValue(refreshedDatasets);

            //then
            expect(ctrl.datasets).toBe(refreshedDatasets);
        }));

        it('should process certification on dataset', inject(function (DatasetService) {
            //when
            ctrl.processCertification(datasets[0]);

            //then
            expect(DatasetService.processCertification).toHaveBeenCalledWith(datasets[0]);
        }));

        it('should toogle dataset favorite flag', inject(function ($rootScope, DatasetService) {
            //given
            var dataset = {name: 'Customers (50 lines)', id: 'aA2bc348e933bc2', favorite: false};

            //when
            ctrl.toggleFavorite(dataset);
            $rootScope.$apply();

            //then
            expect(DatasetService.toggleFavorite).toHaveBeenCalledWith(dataset);
        }));
    });
});
