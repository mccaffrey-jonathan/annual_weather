function setOnPrinting(beforePrint, afterPrint) {
    if (window.matchMedia) {
        var mediaQueryList = window.matchMedia('print');
        mediaQueryList.addListener(function(mql) {
            if (mql.matches) {
                beforePrint();
            } else {
                afterPrint();
            }
        });
    }

    window.onbeforeprint = beforePrint;
    window.onafterprint = afterPrint;
}

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

function formatEmpty(d) {
    return "";
}

function formatFilter(fmt, d) {
    if (d % 10 == 0) {
        return fmt(d);
    } else {
        return "";
    }
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
                (chartDims.windowHeight - 250).toString() +
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

function remeasureBuckets(buckets, dims) {

    // TODO use dims directly
    var x = dims.x,
        y = dims.y,
        barWidth = dims.barWidth,
        barSpace = dims.barSpace;

    // TODO do we really need the padding for box hit-detection
    // or are we just calculating sizes wrong?
    // To find out you could draw the boxes with color bg
    var textSpaceFromBucket = 5,
        textSpaceFromBarSide = 5, hoverRectPad = 5,
        centerTextInColumn = centerTextInColumnOfWidth(barSpace);

    var bucket = buckets.selectAll(".bucket");
    bucket.attr("transform", function(d) {
            return "translate(" + x(d.key) + ",0)";
        })

    bucket.selectAll("g")
        .attr("transform", function(d) {
            return "translate(" + barSpace/2 + ",0)";
        });

    bucket.select(".hover-bg")
      .attr("width", barSpace+2*hoverRectPad)
      .attr("height", function (d) {
              return y(d.value.EMNT) - y(d.value.EMXT);
      })
      .attr("x", -barSpace/2 - hoverRectPad)
      .attr("y", function(d) {
          return y(d.value.EMXT);
      });

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
    bucket.select(".middle")
      .attr("x1", -barWidth/2)
      .attr("x2", barWidth/2)
      .attr("y1", function(d) {
          return y(d.value.MNTM);
      })
      .attr("y2", function(d) {
          return y(d.value.MNTM);
      });

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
}

function updateBuckets(buckets,  data) {
    var bucket = buckets.selectAll(".bucket").data(data);

    var bucketEnter = bucket.enter()
        .append("g")
        .classed("bucket", true)
        .append("g");

    bucketEnter.append("rect").attr("class", "hover-bg");
    bucketEnter.append("rect").attr("class", "box");
    bucketEnter.append("line").attr("class", "middle");
    bucketEnter.append("line").attr("class", "whisker-line top");
    bucketEnter.append("line").attr("class", "whisker-line bot");
    bucketEnter.append("line").attr("class", "whisker-end top");
    bucketEnter.append("line").attr("class", "whisker-end bot");

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

    buckets.classed("animate-in", false);
    buckets.classed("animate-in", true);
}

function remeasureChart(cb) {

    var windowHeight = $(window).height(),
        windowWidth = screen.width,
            //$(window).width(),
        numBuckets = 12,
        yAxisTextOffset = 45,
        margin = {top: 20, right: 40, bottom: 30, left: yAxisTextOffset},

       // width = 960 - margin.left - margin.right,
        width = windowWidth - margin.left - margin.right,
        // height = 500 - margin.top - margin.bottom,
        height = windowHeight - margin.top - margin.bottom,
        barSpace = Math.floor(width / numBuckets) - 1,
        barWidth = (3*barSpace)/4;

    console.log(windowWidth);
    console.log(windowHeight);

    var centerTextInColumn = centerTextInColumnOfWidth(barSpace);

    var x = d3.scale.linear()
        .domain([0, 12])
        .range([0, width]);

    // Inverted range, Bigger is up
    var y = d3.scale.linear()
        .domain([-20, 120])
        .range([height, 0]);

    // An SVG element with a bottom-right origin.
    var outerSvg = d3.select(".result svg");
        // .attr("width", width + margin.left + margin.right)
        // .attr("height", height + margin.top + margin.bottom);

    var svg = outerSvg.select('g')
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // Print axis
    // tickSize width drives the ticks across the entire chart
    {
        var yAxis = d3.svg.axis()
            .scale(y)
            .tickValues(_.range(-10, 120, 10))
            .tickSize(width)
            .tickFormat(formatEmpty)
            .orient("right");

        svg.select("g.y.axis.print").call(yAxis);
    }

    // Classy thermometer y axis
    {
        var base = 2;
        var growing = 6;

        var r = _.range(-20, 121, 1);

        var littleAxis = d3.svg.axis()
            .scale(y)
            .tickValues(_.filter(r, function(t) {
                return (t % 10) != 0 && (t % 2) == 0;
            }))
            .tickSize(base + growing)
            .tickFormat(formatEmpty)
            .orient("left");

        var gLittle = svg.select("g.y.axis.little")
            .call(littleAxis);


        var middleAxis = d3.svg.axis()
            .scale(y)
            .tickValues(_.filter(r, function(t) {
                return (t % 10) == 0 && (t % 20) != 0;
            }))
            .tickSize(base + 2*growing)
            .tickFormat(formatEmpty)
            .orient("left");

        var gMiddle = svg.select("g.y.axis.medium")
            .call(middleAxis);


        var bigAxis = d3.svg.axis()
            .scale(y)
            .tickValues(_.filter(r, function(t) {
                return (t % 20) == 0;
            }))
            .tickSize(base + 3*growing)
            .orient("left");

        var gBig = svg.select("g.y.axis.big")
            .call(bigAxis);

        gBig.selectAll("text")
            .attr("x", -(base + 3*growing))
            .attr("dy", -4)
            .style("text-anchor", "end");
    }

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

    gx.selectAll("line").filter(function(d) { return d; })
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
    var svg = d3.select(".result svg")
        .append("g");
    var gx = svg.append("g").attr("class", "x axis");
    var gy = svg.append("g").attr("class", "y axis little");
    var gy = svg.append("g").attr("class", "y axis medium");
    var gy = svg.append("g").attr("class", "y axis big");
    var gy = svg.append("g").attr("class", "y axis print");

    appendLabel(svg);

    svg.append("g").classed("buckets", true)
}

function showLoading() {
    d3.selectAll('.loading').classed({'hidden': false});
}

function hideLoading() {
    d3.selectAll('.loading').classed({'hidden': true});
}

function loadData(cb) {
    $.ajax({
        url: "search",
        type: "get",
        // data: {q: "New York, NY"},
        data: {q: "Redding, CA"},
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
    showLoading();
    async.parallel({
        data: loadData,
        chartDims: remeasureChart,
    }, function (err, res) {
        var svg = d3.select('.result svg');
        var buckets = svg.select('g.buckets');
        hideLoading();
        updateBuckets(buckets, res.data);
        remeasureBuckets(buckets, res.chartDims);
        remeasureLabel(svg, res.chartDims);

        d3.selectAll('.result svg .axis text')
            .classed("animate-in", true);

        d3.selectAll('.result svg .label')
            .classed("animate-in", true);
    });
}

function onPageResize() {
    async.series({
        chartDims: remeasureChart,
    }, function (err, res) {
        var svg = d3.select('.result svg');
        var buckets = svg.select('g.buckets');
        remeasureBuckets(buckets, res.chartDims);
        remeasureLabel(svg, res.chartDims);
    });
}

function loggingWrapper(func, log) {
    return function() {
        log();
        func();
    }
}

$(document).ready(onPageLoad);
$(window).resize(onPageResize);

setOnPrinting(onPageResize, onPageResize);
