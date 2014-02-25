function init() {
    $.get("data", function(data) {
        // TODO start graph setup async from data fetching
        // $(".result").html(JSON.stringify(data));

        var margin = {top: 20, right: 40, bottom: 30, left: 20},
            width = 960 - margin.left - margin.right,
            height = 500 - margin.top - margin.bottom,
            barSpace = Math.floor(width / data.length) - 1,
            barWidth = (3*barSpace)/4;

        var x = d3.scale.linear()
            .domain([0, 12])
            .range([0, width]);

        var y = d3.scale.linear()
            .domain([0, 1000])
            .range([0, height]);

        // An SVG element with a bottom-right origin.
        var svg = d3.select(".result").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        // Convert strings to numbers.
        data.forEach(function(d) {
            d.key = +d.key;
            // d.value.MNTM = +d.value.MNTM;
            // d.value.MMXT = +d.value.MMXT;
            // d.value.MNTM = +d.value.MMNT;
            // d.value.EMXT = +d.value.EMXT;
            // d.value.EMNT = +d.value.EMNT;
        });

        var bucket = svg.selectAll(".bucket")
            .data(data);

        var bucketEnter =
            bucket.enter()
            .append("g")
            .attr("transform", function(d) {
                return "translate(" + x(d.key) + ",0)";
            })
            .append("g")
            .attr("class", "bucket")
            // Center on middle of bar..
            .attr("transform", function(d) {
                return "translate(" + barSpace/2 + ",0)";
            });

        bucketEnter.append("rect").attr("class", "box");
        bucket.select(".box")
          .attr("width", barWidth)
          .attr("height", function (d) {
                  return y(d3.mean(d.value.MMXT) - d3.mean(d.value.MMNT));
          })
          .attr("x", -barWidth/2)
          .attr("y", function(d) {
              return y(d3.mean(d.value.MMNT));
          });

        // TODO these pinch over the rect stroke a bit...
        bucketEnter.append("line").attr("class", "middle");
        bucket.select(".middle")
          .attr("x1", -barWidth/2)
          .attr("x2", barWidth/2)
          .attr("y1", function(d) {
              return y(d3.mean(d.value.MNTM));
          })
          .attr("y2", function(d) {
              return y(d3.mean(d.value.MNTM));
          });

        bucketEnter.append("line").attr("class", "whisker-line top");
        bucketEnter.append("line").attr("class", "whisker-line bot");
        bucket.selectAll(".whisker-line")
          .attr("x1", 0)
          .attr("x2", 0)
          .attr("stroke-dasharray", "3, 2");

        bucket.selectAll(".whisker-line.top")
          .attr("y1", function (d) {
              return y(d3.mean(d.value.MMXT));
          })
          .attr("y2", function(d) {
              return y(d3.mean(d.value.EMXT));
          });

        bucket.selectAll(".whisker-line.bot")
          .attr("y1", function (d) {
              return y(d3.mean(d.value.MMNT));
          })
          .attr("y2", function(d) {
              return y(d3.mean(d.value.EMNT));
          });

        bucketEnter.append("line").attr("class", "whisker-end top");
        bucketEnter.append("line").attr("class", "whisker-end bot");
        bucket.selectAll(".whisker-end")
          .attr("x1", -barSpace/4)
          .attr("x2", barSpace/4);

        bucket.selectAll(".whisker-end.top")
          .attr("y1", function (d) {
              return y(d3.mean(d.value.EMXT));
          })
          .attr("y2", function(d) {
              return y(d3.mean(d.value.EMXT));
          });

        bucket.selectAll(".whisker-end.bot")
          .attr("y1", function (d) {
              return y(d3.mean(d.value.EMNT));
          })
          .attr("y2", function(d) {
              return y(d3.mean(d.value.EMNT));
          });


    });
}

$(document).ready(init);
