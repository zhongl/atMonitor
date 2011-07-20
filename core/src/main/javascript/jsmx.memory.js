var j4p = new Jolokia({
	url : "http://localhost:7777/jolokia",
	jsonp : true
});

var chart;

function requestData() {
	j4p.request({
		type : "read",
		mbean : "java.lang:type=Memory",
		attribute : "HeapMemoryUsage"
	}, {
		success : function(resp) {
			chart.yAxis.max = resp.value.max
			chart.yAxis.min = resp.value.init
			var value = resp.value.used / (1024 * 1024);
			var time = resp.timestamp * 1000;
			var series = chart.series[0], shift = series.data.length > 20;
			var point = [ time, value ];
			chart.series[0].addPoint(point, true, shift);
			setTimeout(requestData, 1000);
		}
	});

}

function run() {

	chart = new Highcharts.Chart({
		chart : {
			renderTo : 'memory',
			defaultSeriesType : 'spline',
			events : {
				load : requestData
			}
		},
		title : {
			text : 'MEMORY'
		},
		xAxis : {
			type : 'datetime',
			tickPixelInterval : 150
		},
		yAxis : {
			title : {
				text : 'Value'
			},
			plotLines : [ {
				value : 0,
				width : 1,
				color : '#808080'
			} ]
		},
		tooltip : {
			formatter : function() {
				return '<b>' + this.series.name + '</b><br/>'
						+ Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x)
						+ '<br/>' + Highcharts.numberFormat(this.y, 2);
			}
		},
		series : [ {
			name : 'Heap Memory Usage',
			data : []
		} ]
	});

}

function doGc() {
	j4p.execute("java.lang:type=Memory", "gc", {
		success : function() {
			alert("Garbage collection performed");
		}
	});
}

$(document).ready(function() {
	$("#gc").click(doGc);
	run();
});