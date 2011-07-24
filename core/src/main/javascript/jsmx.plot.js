/**
 * @param {String}
 *          id
 * @param {String}
 *          mbean
 * @param {String}
 *          attribute
 * @param {Function}
 *          update
 * @param {String}
 *          Optional path
 * @returns {MBeanAttributeRealTimePolt}
 */
function MBeanAttributeRealTimePolt(id, mbean, attribute, update, path,
    valueUnit) {
  var chart = new Highcharts.Chart({
    chart : {
      renderTo : id
    },
    title : {
      text : mbean + "/" + attribute
    },
    xAxis : {
      type : "datetime"
    },
    yAxis : {
      title : {
        text : null
      },
      labels : {
        formatter : function() {
          return (valueUnit) ? this.value + valueUnit : this.value;
        }
      }
    },
    series : [ {
      name : path || "",
      data : []
    } ]
  });

  this.collectAndUpdateBy = function(jolokia) {
    jolokia.request({
      type : "read",
      mbean : mbean,
      attribute : attribute,
      path : path || ""
    }, {
      success : function(response) {
        update(chart.series[0], chart.yAxis[0], response);
      },
      timeout : 2000, // 2 seconds
      error : function(response) {
        alert("Error: " + response.error);
      },
      ajaxError : function(error) {
        alter("Ajax error: " + error);
      }
    });
  };
}

function toMegabyte(bytes) {
  return bytes / (1024 * 1024);
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
                        + '" style="width: 400px; height: 200px; margin: 5 5; float: left;"></div>');
            var plot = new MBeanAttributeRealTimePolt(attribute, mbean,
                attribute, function(serie, yAxis, response) {
                  var shift = serie.data.length > 5;
                  serie.addPoint({
                    x : response.timestamp * 1000,
                    y : response.value
                  }, true, shift);
                });

            plots.push(plot);
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

      var heapMemoryUsagePolt = new MBeanAttributeRealTimePolt("memory",
          "java.lang:type=Memory", "HeapMemoryUsage", function(serie, yAxis,
              response) {
            var usage = response.value;
            // yAxis.setExtremes(toMegabyte(usage.init), toMegabyte(usage.max));
            var shift = serie.data.length > 5;

            serie.addPoint({
              x : response.timestamp * 1000,
              y : toMegabyte(usage.used)
            }, true, shift);
          }, "", " MB");

      var systemLoadAveragePolt = new MBeanAttributeRealTimePolt("load",
          "java.lang:type=OperatingSystem", "SystemLoadAverage", function(
              serie, yAxis, response) {
            var shift = serie.data.length > 5;
            serie.addPoint({
              x : response.timestamp * 1000,
              y : response.value
            }, true, shift);
          });

      plots.push(heapMemoryUsagePolt);
      plots.push(systemLoadAveragePolt);

      function collectAndUpdate() {
        $.each(plots, function(index, plot) {
          plot.collectAndUpdateBy(j4p);
        });
        setTimeout(collectAndUpdate, updateInterval * 1000);
      }

      collectAndUpdate();
    });
