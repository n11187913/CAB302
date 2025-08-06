from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Any, Dict, Iterable
import matplotlib.pyplot as plt
import os

from util.dates import parse_to_local, date_range


@dataclass
class BurndownChartDataSeries:
    jmdsojdsiojds
    name: str
    data: Dict[datetime, int]
    format: Dict[str, Any]


def default_ideal_trendline_format() -> Dict[str, Any]:
    return dict(
        color="grey",
        linestyle=(0, (5, 5))
    )


@dataclass
class BurndownChartData:
    sprint_name: str
    utc_chart_start: datetime
    utc_chart_end: datetime
    utc_sprint_start: datetime
    utc_sprint_end: datetime
    total_points: int
    series: Iterable[BurndownChartDataSeries]
    points_label: str = "Outstanding Points"
    ideal_trendline_format: Dict[str, Any] = field(
        default_factory=default_ideal_trendline_format
    )


class BurndownChart:

    def __init__(self, data: BurndownChartData):
        self.data = data

    def __get_chart_dates(self) -> list[datetime]:
        """Return a list of dates between start and end (inclusive)."""
        return date_range(self.data.utc_chart_start, self.data.utc_chart_end)

    def __get_mondays(self, chart_dates: list[datetime]) -> list[int]:
        """Return the indices in chart_dates that correspond to Mondays."""
        return [
            i for i, date in enumerate(chart_dates)
            if parse_to_local(date).weekday() == 0
        ]

    def __prepare_chart(self):
        chart_dates = self.__get_chart_dates()

        # Plot each data series
        for series in self.data.series:
            series_dates = [chart_dates.index(date)
                            for date in series.data.keys()
                            if date in chart_dates]
            series_points = [series.data[date]
                             for date in series.data.keys()
                             if date in chart_dates]
            plt.plot(series_dates, series_points, label=series.name, **series.format)

        # Chart appearance
        plt.title(f"{self.data.sprint_name}: Burndown Chart")
        plt.ylabel(self.data.points_label)
        plt.xlabel("Date")

        # Axis limits
        plt.ylim(ymin=0, ymax=self.data.total_points * 1.1)
        plt.xlim(0, len(chart_dates) - 1)

        # X-axis ticks only on Mondays
        monday_indices = self.__get_mondays(chart_dates)
        monday_labels = [
            str(parse_to_local(chart_dates[i]))[:10] for i in monday_indices
        ]
        plt.xticks(monday_indices, monday_labels, rotation=90)

        # Ideal trendline
        sprint_days = (self.data.utc_sprint_end - self.data.utc_sprint_start).days
        if sprint_days > 0:
            plt.axline(
                (chart_dates.index(self.data.utc_sprint_start), self.data.total_points),
                slope=-(self.data.total_points / sprint_days),
                **self.data.ideal_trendline_format
            )

        plt.legend()

    def generate_chart(self, path: str):
        """Generate and save the burndown chart to a file."""
        self.__prepare_chart()
        os.makedirs(os.path.dirname(path), exist_ok=True)
        plt.tight_layout()
        plt.savefig(path)
        plt.close()

    def render(self):
        """Render the chart in a window."""
        self.__prepare_chart()
        plt.tight_layout()
        plt.show()
