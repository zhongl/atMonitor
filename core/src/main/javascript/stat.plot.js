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
  if (!plotOptions)
    plotOptions = new PoltOptions();
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

var j4p = new Jolokia({
  url : "http://localhost:7777/jolokia",
  jsonp : true
});

var updateInterval = 3;

var plots = [];
$(document).ready(function() {
  attachUpdateIntervalEvent();

  var options = {
    chart : {
      renderTo : "avg"

    },
    title : {
      text : "Invacation Average Elapse"
    },
    xAxis : {
      type : "datetime"
    },
    yAxis : {},
    series : [ {
      name : "caller0",
      data : []
    }, {
      name : "caller1",
      data : []
    }, {
      name : "callser2",
      data : []
    } ]

  };

  var avg = new Highcharts.Chart(options);

  function collectAndUpdate() {
    j4p.request({
      type : "read",
      mbean : "jsmx:type=Performance",
      attribute : "statistics"
    }, {
      success : function(response) {
        $("#cnt").html(response.value);
        var lines = response.value.split('\n');
        var len = lines.length;
        var ad = [];
        for ( var i = 0; i < len; i++) {
          var line = lines[i];
          var length = line.length;
          if (line[length - 1] == ":") {
            $("#title").html(line.substr(0, length - 2));
          }
           var ma = line.match(/avg: ([\d]+),([\d]+)ns/);
          if (ma) {
             ad.push(parseInt(ma[1]) * 1000 + parseInt(ma[2]));
          }
        }
        console.log(ad);
        var shift = avg.series[0].data.length > 5;
        for ( var j = 0; j < ad.length; j++)
          avg.series[j].addPoint({
            x : response.timestamp * 1000,
            y : ad[j]
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

    setTimeout(collectAndUpdate, updateInterval * 1000);
  }

  collectAndUpdate();
});
