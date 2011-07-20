var j4p = new Jolokia({
	url : "http://localhost:7777/jolokia",
	jsonp : true
});

var data = [];

// var options = {};

function run() {
	j4p.request({
		type : "read",
		mbean : "java.lang:type=Memory",
		attribute : "HeapMemoryUsage"
	}, {
		success : function(resp) {
			var value = resp.value.used / (1024 * 1024);
			var time = resp.timestamp * 1000;

			if (data.length == 10)
				data.shift();
			data.push([ time, value ]);

			$.plot($("#memory"), [ data ], {
				xaxis : {
					mode : "time"
				},
				grid : {
					backgroundColor : {
						colors : [ "#fff", "#eee" ]
					}
				}
			});
			setTimeout(run, 1000);
		}
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