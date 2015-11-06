(function() {
    'use strict';

    var gridState = {dataView: new Slick.Data.DataView({inlineFilters: false})};

    /**
     * @ngdoc service
     * @name data-prep.services.state.service:GridStateService
     * @description Grid state service. Manage the grid part state
     */
    function GridStateService() {
        return {
            reset: reset,

            setColumnFocus: setColumnFocus,
            setData: setData,
            setFilter: setFilter,
            setGridSelection: setGridSelection,
            updateSelectedColumnsStatistics: updateSelectedColumnsStatistics

        };

        /**
         * @ngdoc method
         * @name updateLinesCount
         * @methodOf data-prep.services.state.service:GridStateService
         * @param {object} data The grid data
         * @description Update the number of lines statistics
         */
        function updateLinesCount(data) {
            gridState.nbLines = gridState.dataView.getLength();
            gridState.nbTotalLines = data.records.length;
            gridState.displayLinesPercentage = (gridState.nbLines * 100 / gridState.nbTotalLines).toFixed(0);
        }


        function updateSelectedColumnsStatistics(filteredRecords) {
            if(gridState.selectedColumn) {
                var filteredRecordsValues = _.pluck(filteredRecords, gridState.selectedColumn.id);
                _.forEach(gridState.selectedColumn.statistics.frequencyTable, function(frequency) {
                    frequency.filteredValue = _.filter(filteredRecordsValues, function(value){ return value === frequency.data; }).length;
                });
            }
        }

        /**
         * @ngdoc method
         * @name setFilter
         * @methodOf data-prep.services.state.service:GridStateService
         * @param {array} filters The filters
         * @param {object} data The grid data
         * @description Update the grid filters
         */
        function setFilter(filters, data) {
            /**
             * @ngdoc method
             * @name allFilterFn
             * @param {object} item The item to test
             * @param {object} args Object containing the filters predicates
             * @description Filter function. It iterates over all filters and return if the provided item fit the predicates
             * @returns {boolean} True if the item pass all the filters
             */
            var allFilterFn = function allFilterFn(item, args) {
                //init filters with actual data
                var initializedFilters = _.map(args.filters, function (filter) {
                    return filter(data);
                });
                //execute each filter on the value
                for (var i = 0; i < initializedFilters.length; i++) {
                    var filter = initializedFilters[i];
                    if (!filter(item)) {
                        return false;
                    }
                }
                return true;
            };

            gridState.dataView.beginUpdate();
            gridState.dataView.setFilterArgs({filters: _.map(filters, 'filterFn')});
            gridState.dataView.setFilter(allFilterFn);
            gridState.dataView.endUpdate();

            updateLinesCount(data);
        }

        /**
         * @ngdoc method
         * @name setColumnFocus
         * @methodOf data-prep.services.state.service:GridStateService
         * @param {string} columnId The column id to focus on
         * @description Set the column id to focus on
         */
        function setColumnFocus(columnId) {
            gridState.columnFocus = columnId;
        }

        /**
         * @ngdoc method
         * @name setData
         * @methodOf data-prep.services.state.service:GridStateService
         * @param {object} data The data
         * @description Set new data in the grid
         */
        function setData(data) {
            gridState.dataView.beginUpdate();
            gridState.dataView.setItems(data.records, 'tdpId');
            gridState.dataView.endUpdate();

            updateLinesCount(data);
            updateSelectedColumn(data);
            var dataGridFilteredRecords = [];
            for(var i=0; i <gridState.dataView.getLength(); i++) {
                dataGridFilteredRecords.push(gridState.dataView.getItem(i));
            }
            updateSelectedColumnsStatistics(dataGridFilteredRecords);
        }

        /**
         * @ngdoc method
         * @name updateSelectedColumn
         * @methodOf data-prep.services.state.service:GridStateService
         * @param {object} data The data
         * @description Determine the selected column from the new data
         */
        function updateSelectedColumn(data) {
            //in preview we do not change anything
            if(data.preview) {
                return;
            }

            //if there is already a selected column, we update the column metadata to reference one of the new columns
            if(gridState.selectedColumn && data.columns) {
                gridState.selectedColumn = _.find(data.columns, {id: gridState.selectedColumn.id}) || data.columns[0];
            }
            //the first column is selected by default
            else {
                gridState.selectedColumn = data.columns[0];
            }
        }

        /**
         * @ngdoc method
         * @name setGridSelection
         * @methodOf data-prep.services.state.service:GridStateService
         * @param {object} column The column metadata
         * @param {number} line The line number
         * @description Set the actual selected column and line
         */
        function setGridSelection(column, line) {
            gridState.selectedColumn = column;
            gridState.selectedLine = line;
        }

        /**
         * @ngdoc method
         * @name reset
         * @methodOf data-prep.services.state.service:GridStateService
         * @description Reset the grid internal values
         */
        function reset() {
            gridState.columnFocus = null;
            gridState.selectedColumn = null;
            gridState.selectedLine = null;
        }
    }

    angular.module('data-prep.services.state')
        .service('GridStateService', GridStateService)
        .constant('gridState', gridState);
})();