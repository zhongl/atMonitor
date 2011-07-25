/**
 * @param {String}
 *          mbean
 * @param {String}
 *          attribute
 * @param {String}
 *          path, Optional
 * @returns {MBeanAttributeCoordinate}
 */
function MBeanAttributeCoordinate(mbean, attribute, path) {
  this.mbean = function() {
    return mbean;
  };
  this.attribute = function() {
    return attribute;
  };
  this.path = function() {
    return path ? path : "";
  };
}

/**
 * @param {String}
 *          yAxisLabel
 * @param {Function}
 *          xAxisValueConvert
 * @param {Number}
 *          xAxisValueKeep
 * @returns {PoltOptions}
 */
function PoltOptions(yAxisLabel, xAxisValueConvert, xAxisValueKeep) {
  var defaultConvert = function(value) {
    return value;
  };

  this.yAxisLabel = function() {
    return yAxisLabel ? yAxisLabel : null;
  };
  this.xAxisValueConvert = xAxisValueConvert ? xAxisValueConvert
      : defaultConvert;
  this.xAxisValueKeep = function() {
    return xAxisValueKeep ? xAxisValueKeep : 5;
  };
}

/**
 * @param {String}
 *          placeholder, id of document element.
 * @param {MBeanAttributeCoordinate}
 *          mBeanAttributeCoordinate
 * @param {PoltOptions}
 *          plotOptions
 * @returns {MBeanAttributeRealTimePolt}
 */
function MBeanAttributeRealTimePolt(placeholder, mBeanAttributeCoordinate,
    plotOptions) {
  if (!plotOptions) plotOptions = new PoltOptions();
  var options = {
    chart : {
      renderTo : placeholder
    },
    title : {
      text : mBeanAttributeCoordinate.attribute()
    },
    xAxis : {
      type : "datetime"
    },
    yAxis : {
      title : {
        text : plotOptions.yAxisLabel()
      }
    },
    series : [ {
      name : mBeanAttributeCoordinate.path(),
      data : []
    } ]

  };

  var chart = new Highcharts.Chart(options);

  this.collectAndUpdateBy = function(jolokia) {
    jolokia.request({
      type : "read",
      mbean : mBeanAttributeCoordinate.mbean(),
      attribute : mBeanAttributeCoordinate.attribute(),
      path : mBeanAttributeCoordinate.path()
    }, {
      success : function(response) {
        var serie = chart.series[0];
        var shift = serie.data.length > plotOptions.xAxisValueKeep();
        var value = plotOptions.xAxisValueConvert(response.value);
        serie.addPoint({
          x : response.timestamp * 1000,
          y : value
        }, true, shift);
      },
      timeout : 2000, // 2 seconds
      error : function(response) {
        alert("Error: " + response.error);
      },
      ajaxError : function(error) {
        alert("Ajax error: " + error);
      }
    });
  };
}

function attachGcEvent() {
  $("#gc").click(function() {
    j4p.execute("java.lang:type=Memory", "gc", {
      success : function() {
        alert("Garbage collection performed");
      }
    });
  });
}

function attachUpdateIntervalEvent() {
  $("#updateInterval").val(updateInterval).change(function() {
    var v = $(this).val();
    updateInterval = (v < 1) ? 1 : v;
    $(this).val(updateInterval);
  });
}

function attachAddNewWatchEvent() {
  $("#newWatch")
      .click(
          function() {
            var mbean = $("#mbean").val();
            var attribute = $("#attribute").val();

            $("#dashboard")
                .append(
                    '<div id="'
                        + attribute
                        + '" style="width: 600px; height: 200px; margin: 5 5; float: left;"></div>');
            var coordinate = new MBeanAttributeCoordinate(mbean, attribute);
            plots.push(new MBeanAttributeRealTimePolt(attribute, coordinate));
          });
}

var j4p = new Jolokia({
  url : "http://localhost:7777/jolokia",
  jsonp : true
});

var updateInterval = 3;

var plots = [];
$(document).ready(
    function() {
      attachGcEvent();
      attachUpdateIntervalEvent();
      attachAddNewWatchEvent();

      var heapMemoryUsage = new MBeanAttributeCoordinate(
          "java.lang:type=Memory", "HeapMemoryUsage", "used");
      var heapMemoryUsagePlotOptions = new PoltOptions("Unit: MB", function(
          value) {
        return value / (1024 * 1024);
      });

      plots.push(new MBeanAttributeRealTimePolt("memory", heapMemoryUsage,
          heapMemoryUsagePlotOptions));
      plots.push(new MBeanAttributeRealTimePolt("load",
          new MBeanAttributeCoordinate("java.lang:type=OperatingSystem",
              "SystemLoadAverage")));

      function collectAndUpdate() {
        $.each(plots, function(index, plot) {
          plot.collectAndUpdateBy(j4p);
        });
        setTimeout(collectAndUpdate, updateInterval * 1000);
      }

      collectAndUpdate();
    });
