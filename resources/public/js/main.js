var monthDictionary = {
    0: "Jan",
    1: "Feb",
    2: "Mar",
    3: "Apr",
    4: "May",
    5: "Jun",
    6: "Jul",
    7: "Aug",
    8: "Sep",
    9: "Oct",
    10: "Nov",
    11: "Dec"
};

function isEven(n) {
    return (n % 2) == 0;
}

function isOdd(n) {
    return (n % 2) == 1;
}

function formatMonths(d) {
    return monthDictionary[d]
}

function centerTextInColumnOfWidth(w) {
    return function(sel) {
        return sel.attr("x", w/2)
            .style("width", w)
            .style("text-anchor", "middle");
    }
}

function formatFahrenheit(d) {
  return d + "\u00B0F";
}

// TODO I really prefer just in front of decimal
var formatNumber = d3.format(".1f");

var formatNumberNoDecimal = d3.format(".0f");

function ncdcToFahrenheit(ncdc) {
    return ((9*ncdc)/50) + 32;
}

function ncdcToFahrenheitStr(accessor) {
    return function (d) {
        formatFahrenheit(ncdcToFahrenheitStr(accessor(d)));
    }
}

function appendLabel(svg) {
    svg.append("g")
        .classed("label", true)
        .append("foreignObject")
        .classed("war-foreign-object", true)
        .append("xhtml:body")
        .append("span")
        .append("div");
}

function remeasureLabel(svg, chartDims) {
    var cityAndState = "Los Angeles, CA";
    var latAndLong = "43\u00B0N 37\u00B0W";
    var climate = "Temperate Climate";
    var flavorText = "300 Days of Sunshine";
    var url = "wby.com/LosAngelesCA";

    // TODO templates
    bodyString = 
        '<div class="labeltop">' +
            cityAndState + '<br/>' +
            latAndLong + 
        '</div>' +
        // '<hr/>' +
        '<div class="labelmid">' +
            climate + '<br/>' + 
            flavorText +
        "</div>" +
        // '<hr/>' +
        '<div class="labelbot">' +
            '<a href="' + url + '">' + url + '</a>' +
        "</div>";

    var label = svg.select("g.label")
        .attr("transform", "translate(" +
                (chartDims.windowWidth - 350).toString() +
                ", " +
                (chartDims.windowHeight - 225).toString() +
                ")");

    label.select(".war-foreign-object")
        .attr("width", 500)
        .attr("height", 500)
        .select("body span")
        .style("display", "inline-block")
        .style("text-align", "center")
        .select("div")
        .html(bodyString);
}

// TODO Make these construction functions 
// more declarative and less directly append-ative
// For example, this probably won't support re-size or re-layout well.
// Which is important to you.
//
// But first get this thing off the ground
function updateBuckets(buckets, dims, data) {

    // TODO use dims directly
    var x = dims.x,
        y = dims.y,
        barWidth = dims.barWidth,
        barSpace = dims.barSpace;

    // TODO do we really need the padding for box hit-detection
    // or are we just calculating sizes wrong?
    // To find out you could draw the boxes with color bg
    var textSpaceFromBucket = 5,
        textSpaceFromBarSide = 5,
        hoverRectPad = 5,
        centerTextInColumn = centerTextInColumnOfWidth(barSpace);

    var bucket = buckets.selectAll(".bucket").data(data);

    var bucketEnter = bucket.enter()
        .append("g")
        .classed("bucket", true)
        .attr("transform", function(d) {
            return "translate(" + x(d.key) + ",0)";
        })
        .append("g")
        // Center on middle of bar..
        .attr("transform", function(d) {
            return "translate(" + barSpace/2 + ",0)";
        });

    bucketEnter.append("rect").attr("class", "hover-bg");
    bucket.select(".hover-bg")
      .attr("width", barSpace+2*hoverRectPad)
      .attr("height", function (d) {
              return y(d.value.EMNT) - y(d.value.EMXT);
      })
      .attr("x", -barSpace/2 - hoverRectPad)
      .attr("y", function(d) {
          return y(d.value.EMXT);
      });

    [
        "extreme-max",
        "extreme-min",
        "mean-max",
        "mean-min",
        "mean",
    ].forEach(function(classes) {
        bucketEnter.append("text")
          .attr("class", "bucket-label " + classes);
        bucketEnter.append("line")
          .attr("class", "bucket-label " + classes);
    });

    bucketEnter.append("rect").attr("class", "box");
    bucket.select(".box")
      .attr("width", barWidth)
      .attr("height", function (d) {
              return y(d.value.MMNT) - y(d.value.MMXT);
      })
      .attr("x", -barWidth/2)
      .attr("y", function(d) {
          return y(d.value.MMXT);
      });

    // TODO these pinch over the rect stroke a bit...
    bucketEnter.append("line").attr("class", "middle");
    bucket.select(".middle")
      .attr("x1", -barWidth/2)
      .attr("x2", barWidth/2)
      .attr("y1", function(d) {
          return y(d.value.MNTM);
      })
      .attr("y2", function(d) {
          return y(d.value.MNTM);
      });

    bucketEnter.append("line").attr("class", "whisker-line top");
    bucketEnter.append("line").attr("class", "whisker-line bot");
    bucket.selectAll(".whisker-line")
      .attr("x1", 0)
      .attr("x2", 0);

    bucket.filter(function (d) {return isOdd(d.key); })
        .classed("odd", true);

    bucket.selectAll(".whisker-line.top")
      .attr("y1", function (d) {
          return y(d.value.MMXT);
      })
      .attr("y2", function(d) {
          return y(d.value.EMXT);
      });

    bucket.selectAll(".whisker-line.bot")
      .attr("y1", function (d) {
          return y(d.value.MMNT);
      })
      .attr("y2", function(d) {
          return y(d.value.EMNT);
      });

    bucketEnter.append("line").attr("class", "whisker-end top");
    bucketEnter.append("line").attr("class", "whisker-end bot");

    bucket.selectAll(".whisker-end")
      .attr("x1", -barSpace/4)
      .attr("x2", barSpace/4);
        
    bucket.selectAll(".whisker-end.top")
      .attr("y1", function (d) {
          return y(d.value.EMXT);
      })
      .attr("y2", function(d) {
          return y(d.value.EMXT);
      });

    bucket.selectAll(".whisker-end.bot")
      .attr("y1", function (d) {
          return y(d.value.EMNT);
      })
      .attr("y2", function(d) {
          return y(d.value.EMNT);
      });

    bucket.selectAll("text.bucket-label")
      .attr("x", barWidth/2 + textSpaceFromBarSide)
      .style("text-anchor", "start");
    bucket.selectAll("line.bucket-label")
    // TODO: is there a way to have an element refer to
    // a higher-level of scene graph ? 
      .attr("x1", function (d) { return -x(d.key) - barSpace/2; })
      .attr("x2", barWidth/2 + textSpaceFromBarSide);

    var setBucketLabelTemp = function (sel, accessor) {
        var yAccessor = _.compose(y, accessor);
        sel.filter('text.bucket-label')
            .text(_.compose(formatFahrenheit,
                        formatNumberNoDecimal,
                        accessor))
            .attr("y", yAccessor);

        sel.filter('line.bucket-label')
            .attr("y1", yAccessor)
            .attr("y2", yAccessor);
    };

    setBucketLabelTemp(d3.selectAll(".extreme-min"), function (d) {
        return d.value.EMNT;
    });
    setBucketLabelTemp(d3.selectAll(".extreme-max"), function (d) {
        return d.value.EMXT;
    });
    setBucketLabelTemp(d3.selectAll(".mean-min"), function (d) {
        return d.value.MMNT;
    });
    setBucketLabelTemp(d3.selectAll(".mean-max"), function (d) {
        return d.value.MMXT;
    });
    setBucketLabelTemp(d3.selectAll(".mean"), function (d) {
        return d.value.MNTM;
    });

    buckets.classed("animate-in", false);
    buckets.classed("animate-in", true);
    
    // var emntLabelSel = bucket.select(".extreme-min.bucket-label")
    //     .text(function (d) {
    //         return formatFahrenheit(formatNumberNoDecimal(d.value.EMNT));
    //     });

    // emntLabelSel.style("opacity", 0);
    // var bbox = emntLabelSel[0][0].getBBox(),
    //     emntLabelHeight = bbox.height;
    // emntLabelSel.style("opacity", 1);

    // emntLabelSel.attr("y", function (d) {
    //     return y(d.value.EMNT);
    // })

}

function remeasureChart(cb) {
    var body = document.body,
        html = document.documentElement;

    var windowHeight = Math.max( body.scrollHeight, body.offsetHeight, 
        html.clientHeight, html.scrollHeight, html.offsetHeight );

    var windowWidth = window.innerWidth ||
        document.documentElement.clientWidth ||
        document.body.clientWidth;

    var numBuckets = 12,
        yAxisTextOffset = 45,
        margin = {top: 20, right: 40, bottom: 30, left: yAxisTextOffset},
       // width = 960 - margin.left - margin.right,
        width = windowWidth - margin.left - margin.right,
        // height = 500 - margin.top - margin.bottom,
        height = windowHeight - margin.top - margin.bottom,
        barSpace = Math.floor(width / numBuckets) - 1,
        barWidth = (3*barSpace)/4;

    var centerTextInColumn = centerTextInColumnOfWidth(barSpace);

    var x = d3.scale.linear()
        .domain([0, 12])
        .range([0, width]);

    // Inverted range, Bigger is up
    var y = d3.scale.linear()
        .domain([-20, 120])
        .range([height, 0]);

    // An SVG element with a bottom-right origin.
    var outerSvg = d3.select(".result svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom);

    var svg = outerSvg.select('g')
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // tickSize width drives the ticks across the entire chart
    var yAxis = d3.svg.axis()
        .scale(y)
        .ticks(20)
        .tickSize(width)
        .tickFormat(formatFahrenheit)
        .orient("right");

    var gy = svg.select("g.y.axis")
        .call(yAxis);

    gy.selectAll("g")
        .filter(function(d) { return d; })
        .classed("minor", true);

    gy.selectAll("g")
        .filter(function(d) { return d === 0; })
        .classed("major", true);

    gy.selectAll("text")
        .attr("x", -yAxisTextOffset)
        .attr("dy", 4);

    var xAxis = d3.svg.axis()
        .scale(x)
        .tickSize(-height)
        .tickSubdivide(true)
        .tickFormat(formatMonths);

    // Add the x-axis.
    var gx = svg.select("g.x.axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);
    
    gx.selectAll("text")
        .filter(isEven).classed("even", true);
    gx.selectAll("text")
        .filter(isOdd).classed("odd", true);

    gx.selectAll("text")
        .call(centerTextInColumn);

    gx.selectAll("g").filter(function(d) { return d; })
        .classed("hidden", true);

    cb(null, {
        x: x,
        y: y,
        barWidth: barWidth,
        barSpace: barSpace,
        windowWidth: windowWidth,
        windowHeight: windowHeight
    });
}

function appendChart() {
    var svg = d3.select(".result").append("svg").append("g");
    var gy = svg.append("g").attr("class", "y axis");
    var gx = svg.append("g").attr("class", "x axis");

    appendLabel(svg);

    svg.append("g").classed("buckets", true)
}

function loadData(cb) {
    $.ajax({
        url: "search",
        type: "get",
        data: {q: "Ithaca, CA"},
        success: function (data) {

            // Convert strings to numbers.
            data.forEach(function(d) {
                d.key = +d.key;
                d.value.MNTM = ncdcToFahrenheit(d3.mean(d.value.MNTM));
                d.value.MMXT = ncdcToFahrenheit(d3.mean(d.value.MMXT));
                d.value.MMNT = ncdcToFahrenheit(d3.mean(d.value.MMNT));
                d.value.EMXT = ncdcToFahrenheit(d3.mean(d.value.EMXT));
                d.value.EMNT = ncdcToFahrenheit(d3.mean(d.value.EMNT));
            });

            cb(null, data);
        },
        error: function (jqXhr, textStatus, errorThrown) {
            cb(errorThrown, null);
        }});
}

function onPageLoad() {
    appendChart();
    async.parallel({
        data: loadData,
        chartDims: remeasureChart,
    }, function (err, res) {
        var svg = d3.select('.result svg');
        var buckets = svg.select('g.buckets');
        updateBuckets(buckets, res.chartDims, res.data);
        remeasureLabel(svg, res.chartDims);

        d3.selectAll('.result svg .axis text')
            .classed("animate-in", true);

        d3.selectAll('.result svg .label')
            .classed("animate-in", true);
    });
}

$(document).ready(onPageLoad);
$(window).resize( );
