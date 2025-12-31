import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Legend , Cell } from 'recharts';
import { ApiService } from '../../services/apiService';

export const StatisticsPage = () => {
    const [statistics, setStatistics] = useState(null);
    const [statisticsLoading, setStatisticsLoading] = useState(false);
    const [yearlyLoading, setYearlyLoading] = useState(false);
    const [selectedTab, setSelectedTab] = useState('people');
    const [selectedWeekday, setSelectedWeekday] = useState('1'); // 월요일 기본값
    const [selectedMonth, setSelectedMonth] = useState(getCurrentMonth()); // 기본값은 이번 달
    const [yearlyData, setYearlyData] = useState([]); // 1년 동안의 월별 데이터를 저장
    const [peopleList, setPeopleList] = useState([]);
    const [placeList, setPlaceList] = useState([]);
    const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
    const weekdays = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
    // 현재 월을 "yyyy-MM" 형식으로 반환하는 함수
    function getCurrentMonth() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1 해줍니다.
        return `${year}-${month}`;
    }

    // 1년 동안의 월별 데이터를 요청하는 함수
 const fetchYearlyData = async () => {
  try {
    setYearlyLoading(true);

    const data = [];
    const currentYear = new Date().getFullYear();

    for (let month = 1; month <= 12; month++) {
      const yearMonth = `${currentYear}-${String(month).padStart(2, '0')}`;
      const monthlyData = await ApiService.getMonthlyScheduleTrend(yearMonth);
      data.push(monthlyData.data);
    }

    setYearlyData(data);
  } catch (error) {
    console.error('Failed to fetch yearly data:', error);
  } finally {
    setYearlyLoading(false);
  }
};


    // 선택된 월에 해당하는 통계를 가져오는 함수
const fetchStatistics = async () => {
  try {
    setStatisticsLoading(true);

    const yearMonth = selectedMonth;

    const peopleSummaryResponse = await ApiService.getPeopleSummary(yearMonth);
    const placeSummaryResponse = await ApiService.getPlaceSummary(yearMonth);

    const peopleListResponse = await ApiService.getPeopleList(yearMonth, 1, 20);
    const placeListResponse = await ApiService.getPlaceList(yearMonth, 1, 20);

    const scheduleDayHourResponse = await ApiService.getScheduleDayHourDistribution();
    const monthlyTrendResponse = await ApiService.getMonthlyScheduleTrend();

    setStatistics({
      peopleSummary: peopleSummaryResponse.data,
      placeSummary: placeSummaryResponse.data,
      scheduleDayHour: scheduleDayHourResponse.data,
      monthlyTrend: monthlyTrendResponse.data,
    });

    setPeopleList(peopleListResponse.data?.content || []);
    setPlaceList(placeListResponse.data?.content || []);
  } catch (error) {
    console.error('Failed to fetch statistics:', error);
  } finally {
    setStatisticsLoading(false);
  }
};

 

    // 월별 데이터를 업데이트 할 때마다 fetch
   useEffect(() => {
  fetchStatistics();
}, [selectedMonth]);

// ✅ 최초 1회: 월별 통계용 1년치 데이터(필요할 때만)
useEffect(() => {
  fetchYearlyData();
}, []);

    // 탭 버튼 스타일
    const tabButtonStyle = {
        padding: '12px 24px',
        fontSize: '16px',
        margin: '0 10px',
        background: 'linear-gradient(135deg, #6b8df2, #85c4ff)',
        color: '#fff',
        border: 'none',
        borderRadius: '20px',
        cursor: 'pointer',
        transition: 'all 0.3s ease',
    };

    const tabButtonActiveStyle = {
        ...tabButtonStyle,
        background: 'linear-gradient(135deg, #85c4ff, #6b8df2)',
        transform: 'scale(1.05)',
    };

    // 월별 스케줄 추이 (꺾은선 그래프)
const renderMonthlyTrend = () => {
  const monthlyTrendData = statistics?.monthlyTrend || [];

  if (monthlyTrendData.length === 0) {
    return <p>월별 데이터가 없습니다.</p>;
  }

  // ✅ 선택된 연도 필터링
  const filteredData = monthlyTrendData.filter(item =>
    item.month.startsWith(String(selectedYear))
  );

  const transformedData = filteredData.map(item => ({
    month: item.month.slice(0, 7),
    scheduleCount: item.scheduleCount,
  }));

  return (
    <div>
      <h3>월별 스케줄 추이</h3>

      {/* ✅ 연도 선택 박스 */}
      <select
        value={selectedYear}
        onChange={(e) => setSelectedYear(e.target.value)}
        style={{
          padding: '8px 16px',
          borderRadius: '8px',
          marginBottom: '20px',
        }}
      >
        {[2023, 2024, 2025].map(year => (
          <option key={year} value={year}>
            {year}년
          </option>
        ))}
      </select>

      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={transformedData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="month" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="scheduleCount" stroke="#8884d8" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

    // 요일별 스케줄 (원형 그래프)
const renderWeekdayDistribution = () => {
    const scheduleData = statistics?.scheduleDayHour || [];

    // 요일별 스케줄 수 합산
    const weekdayScheduleCount = [0, 0, 0, 0, 0, 0, 0]; // 일요일(0) ~ 토요일(6)

    scheduleData.forEach((entry) => {
        weekdayScheduleCount[entry.dayOfWeek] += entry.scheduleCount;
    });

    // 요일별 이름 배열
    const weekdays = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];

    // 요일별 색상 배열
    const weekdayColors = ['#FF8042', '#00C49F', '#FFBB28', '#0088FE', '#FF8042', '#00C49F', '#FFBB28'];

    // 차트에 사용할 데이터 (0인 요일 제외)
    const chartData = weekdayScheduleCount
        .map((count, index) => ({
            dayOfWeek: weekdays[index],
            scheduleCount: count,
            color: weekdayColors[index], // 색상 할당
        }))
        .filter((entry) => entry.scheduleCount > 0); // 스케줄 수가 0인 요일은 제외

    return (
        <div>
            <h3>요일별 스케줄</h3>
            <ResponsiveContainer width="100%" height={300}>
               <PieChart>
  <Pie
    data={chartData}
    dataKey="scheduleCount"
    nameKey="dayOfWeek"
    cx="50%"
    cy="50%"
    outerRadius={100}
  >
    {chartData.map((entry, index) => (
      <Cell key={`cell-${index}`} fill={entry.color} />
    ))}
  </Pie>
  <Tooltip />
  <Legend />
</PieChart>
            </ResponsiveContainer>
            {/* 요일과 카운트 표시 */}
            <div style={{ textAlign: 'center' }}>
                {chartData.map((entry, index) => (
                    <span key={index} style={{ margin: '0 5px' }}>
                        <span
                            style={{
                                display: 'inline-block',
                                width: '10px',
                                height: '10px',
                                backgroundColor: entry.color,
                                marginRight: '5px',
                            }}
                        />
                        {entry.dayOfWeek} : {entry.scheduleCount}
                    </span>
                ))}
            </div>
        </div>
    );
};


const renderYearMonthSelector = () => {
  return (
    <div
      style={{
        display: 'flex',
        gap: '12px',
        alignItems: 'center',
        marginBottom: '24px',
        padding: '12px 16px',
        background: '#f8f9fc',
        borderRadius: '12px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
        width: 'fit-content',
      }}
    >
      {/* 연도 셀렉 */}
      <select
        value={selectedYear}
        onChange={(e) => {
          const year = e.target.value;
          const month = selectedMonth.split('-')[1];
          setSelectedYear(year);
          setSelectedMonth(`${year}-${month}`);
        }}
        style={{
          padding: '8px 14px',
          borderRadius: '8px',
          border: '1px solid #d0d7ff',
          backgroundColor: '#fff',
          fontSize: '14px',
          cursor: 'pointer',
        }}
      >
        {[2023, 2024, 2025].map((year) => (
          <option key={year} value={year}>
            {year}년
          </option>
        ))}
      </select>

      {/* 월 셀렉 */}
      <select
        value={selectedMonth.split('-')[1]}
        onChange={(e) => {
          const month = e.target.value;
          setSelectedMonth(`${selectedYear}-${month}`);
        }}
        style={{
          padding: '8px 14px',
          borderRadius: '8px',
          border: '1px solid #d0d7ff',
          backgroundColor: '#fff',
          fontSize: '14px',
          cursor: 'pointer',
        }}
      >
        {Array.from({ length: 12 }, (_, i) => {
          const month = String(i + 1).padStart(2, '0');
          return (
            <option key={month} value={month}>
              {month}월
            </option>
          );
        })}
      </select>
    </div>
  );
};


//시간대별 통계
const renderHourlyDistribution = () => {
    const scheduleData = statistics?.scheduleDayHour || [];

    // 선택된 요일에 해당하는 데이터만 필터링
    const filteredData = scheduleData.filter((entry) => entry.dayOfWeek === parseInt(selectedWeekday));

    // 3시간 간격으로 데이터를 변환할 배열
    const hourlyScheduleCount = Array(8).fill(0); // 0시-3시, 3시-6시, ..., 21시-24시

    filteredData.forEach((entry) => {
        // 3시간 간격으로 나누어 카운트
        const hourIndex = Math.floor(entry.hourOfDay / 3);
        hourlyScheduleCount[hourIndex] += entry.scheduleCount;
    });

    // 3시간 간격으로 x축을 설정
    const timeLabels = ['0시-3시', '3시-6시', '6시-9시', '9시-12시', '12시-15시', '15시-18시', '18시-21시', '21시-24시'];

    const chartData = hourlyScheduleCount.map((count, index) => ({
        timeRange: timeLabels[index],
        scheduleCount: count,
    }));

    return (
        <div>
            <h3>시간대별 스케줄</h3>
            <div>
                <select
                    value={selectedWeekday}
                    onChange={(e) => setSelectedWeekday(e.target.value)}
                    style={{
                        padding: '10px 20px',
                        borderRadius: '10px',
                        border: '1px solid #ccc',
                        fontSize: '16px',
                        backgroundColor: '#f0f0f0',
                        cursor: 'pointer',
                        marginBottom: '20px',
                        width: 'auto',
                    }}
                >
                    {['월요일', '화요일', '수요일', '목요일', '금요일', '토요일', '일요일'].map((day, index) => (
                        <option key={index} value={index}>
                            {day}
                        </option>
                    ))}
                </select>
            </div>
            <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="timeRange" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="scheduleCount" fill="#8884d8" />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
};


    // 사람 통계 (Top 3 + 나머지 목록)
    const renderPeopleSummary = () => (
        <div>
            <h3>사람 통계</h3>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
                {statistics?.peopleSummary?.top3?.map((person, index) => (
                    <div
                        key={index}
                        style={{
                            width: '30%',
                            padding: '1rem',
                            borderRadius: '8px',
                            backgroundColor: '#ADD8E6',
                            boxShadow: '0 4px 8px rgba(0, 0, 0, 0.1)',
                            textAlign: 'center',
                        }}
                    >
                        <h4>{`Rank : ${index + 1}`}</h4>
                        <p>이름 : {person.name}</p>
                        <p>만난 횟수 : {person.meetCount}</p>
                        <p>총 만난 시간(분) : {person.totalDurationMin}</p>
                        <p>횟수당 평균 만난 시간(분) : {person.avgDurationMin}</p>
                        <p>함께 지출한 금액 : {person.totalAmount}</p>
                        <p>횟수당 평균 지출 금액 : {person.avgAmount}</p>
                    </div>
                ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'center' }}>
  <table
    style={{
      width: '90%',
      borderCollapse: 'collapse',
      textAlign: 'center',
    }}
  >
                    <thead>
                        <tr>
                            <th>이름</th>
                            <th>만난 횟수</th>
                            <th>총 만난 시간</th>
                            <th>횟수당 평균 만난 시간</th>
                            <th>함께 지출한 금액</th>
                            <th>횟수당 평균 지출 금액</th>
                        </tr>
                    </thead>
                    <tbody>
  {peopleList.map((person, index) => (
    <tr key={person.id ?? index} style={{ backgroundColor: index % 2 === 0 ? '#f9f9f9' : '#fff' }}>
      <td>{person.name}</td>
      <td>{person.meetCount}</td>
      <td>{person.totalDurationMin}</td>
      <td>{person.avgDurationMin}</td>
      <td>{person.totalAmount}</td>
      <td>{person.avgAmount}</td>
    </tr>
  ))}
</tbody>
                </table>
            </div>
        </div>
    );

    // 장소 통계 (Top 3 + 나머지 목록)
    const renderPlaceSummary = () => (
        <div>
            <h3>장소 통계</h3>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
                {statistics?.placeSummary?.top3?.map((place, index) => (
                    <div
                        key={index}
                        style={{
                            width: '30%',
                            padding: '1rem',
                            borderRadius: '8px',
                            backgroundColor: '#ADD8E6',
                            boxShadow: '0 4px 8px rgba(0, 0, 0, 0.1)',
                            textAlign: 'center',
                        }}
                    >
                        <h4>{`Rank : ${index + 1}`}</h4>
                        <p>장소 : {place.placeName}</p>
                        <p>방문 횟수 : {place.visitCount}</p>
                        <p>총 머문 시간(분) : {place.totalStayMin}</p>
                        <p>횟수당 평균 머문 시간(분) : {place.avgStayMin}</p>
                        <p>이 장소에서 지출한 금액 : {place.totalAmount}</p>
                        <p>방문당 평균 지출 금액 : {place.avgAmount}</p>
                    </div>
                ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'center' }}>
  <table
    style={{
      width: '90%',
      borderCollapse: 'collapse',
      textAlign: 'center',
    }}
  >
                    <thead>
                        <tr>
                            <th>장소</th>
                            <th>방문 횟수</th>
                            <th>총 머문 시간</th>
                            <th>횟수당 평균 머문 시간</th>
                            <th>방문당 평균 지출 금액</th>
                        </tr>
                    </thead>
                   <tbody>
  {placeList.map((place, index) => (
    <tr key={place.id ?? index} style={{ backgroundColor: index % 2 === 0 ? '#f9f9f9' : '#fff' }}>
      <td>{place.placeName}</td>
      <td>{place.visitCount}</td>
      <td>{place.totalStayMin}</td>
      <td>{place.avgStayMin}</td>
      <td>{place.avgAmount}</td>
    </tr>
  ))}
</tbody>
                </table>
            </div>
        </div>
    );

    // 탭 버튼
    const handleTabClick = (tab) => {
        setSelectedTab(tab);
    };


return (
  <div>
    <h1>통계</h1>

    {/* 탭 버튼 */}
    <div style={{ marginBottom: '2rem' }}>
      <button
        style={selectedTab === 'people' ? tabButtonActiveStyle : tabButtonStyle}
        onClick={() => setSelectedTab('people')}
      >
        사람 통계
      </button>
      <button
        style={selectedTab === 'place' ? tabButtonActiveStyle : tabButtonStyle}
        onClick={() => setSelectedTab('place')}
      >
        장소 통계
      </button>
      <button
        style={selectedTab === 'monthly' ? tabButtonActiveStyle : tabButtonStyle}
        onClick={() => setSelectedTab('monthly')}
      >
        월별 통계
      </button>
      <button
        style={selectedTab === 'weekday' ? tabButtonActiveStyle : tabButtonStyle}
        onClick={() => setSelectedTab('weekday')}
      >
        요일별 통계
      </button>
      <button
        style={selectedTab === 'hour' ? tabButtonActiveStyle : tabButtonStyle}
        onClick={() => setSelectedTab('hour')}
      >
        시간대별 통계
      </button>
    </div>

    {/* ✅ 사람 / 장소 필터는 항상 보이게 */}
    {(selectedTab === 'people' || selectedTab === 'place') && (
    <div style={{ marginBottom: '16px' }}>
    {renderYearMonthSelector()}
    </div>
)}
    {/* ✅ 통계 로딩 */}
    {statisticsLoading && <p>Loading...</p>}

    {/* ✅ 월별 통계 로딩 */}
    {selectedTab === 'monthly' && yearlyLoading && <p>Loading...</p>}

    {/* ✅ 탭별 렌더 */}
    {!statisticsLoading && selectedTab === 'people' && renderPeopleSummary()}
    {!statisticsLoading && selectedTab === 'place' && renderPlaceSummary()}
    {selectedTab === 'monthly' && renderMonthlyTrend()}
    {!statisticsLoading && selectedTab === 'weekday' && renderWeekdayDistribution()}
    {!statisticsLoading && selectedTab === 'hour' && renderHourlyDistribution()}
  </div>
);
};
