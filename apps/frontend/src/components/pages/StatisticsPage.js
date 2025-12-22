import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Legend , Cell } from 'recharts';
import { ApiService } from '../../services/apiService';

export const StatisticsPage = () => {
    const [statistics, setStatistics] = useState(null);
    const [loading, setLoading] = useState(false);
    const [selectedTab, setSelectedTab] = useState('people');
    const [selectedWeekday, setSelectedWeekday] = useState('1'); // 월요일 기본값
    const [selectedMonth, setSelectedMonth] = useState(getCurrentMonth()); // 기본값은 이번 달
    const [yearlyData, setYearlyData] = useState([]); // 1년 동안의 월별 데이터를 저장
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
            setLoading(true);
            const data = [];
            const currentYear = new Date().getFullYear();
            // 1년간의 각 월 데이터 요청
            for (let month = 1; month <= 12; month++) {
                const yearMonth = `${currentYear}-${String(month).padStart(2, '0')}`;
                const monthlyData = await ApiService.getMonthlyScheduleTrend(yearMonth);
                data.push(monthlyData.data);
            }
            setYearlyData(data); // 1년 데이터 저장
        } catch (error) {
            console.error('Failed to fetch yearly data:', error);
        } finally {
            setLoading(false);
        }
    };


    // 선택된 월에 해당하는 통계를 가져오는 함수
 const fetchStatistics = async () => {
        try {
            setLoading(true);
            console.log('Fetching statistics...');
            const yearMonth = new Date().toISOString().slice(0, 7); // 현재 날짜를 "yyyy-MM" 형식으로 변환
            const peopleSummaryResponse = await ApiService.getPeopleSummary(yearMonth);  // month 파라미터로 "yyyy-MM" 형식 전달
            console.log('People summary:', peopleSummaryResponse);
            const placeSummaryResponse = await ApiService.getPlaceSummary(yearMonth); // 마찬가지로 "yyyy-MM"
            console.log('Place summary:', placeSummaryResponse);
            const scheduleDayHourResponse = await ApiService.getScheduleDayHourDistribution();
            console.log('Schedule day hour:', scheduleDayHourResponse);
            const monthlyTrendResponse = await ApiService.getMonthlyScheduleTrend();
            console.log('Monthly trend:', monthlyTrendResponse);

            setStatistics({
                peopleSummary: peopleSummaryResponse.data,
                placeSummary: placeSummaryResponse.data,
                scheduleDayHour: scheduleDayHourResponse.data,
                monthlyTrend: monthlyTrendResponse.data,
            });
        } catch (error) {
            console.error('Failed to fetch statistics:', error);
        } finally {
            setLoading(false);
        }
    };  

    // 월별 데이터를 업데이트 할 때마다 fetch
    useEffect(() => {
        fetchStatistics(); // 선택된 월에 해당하는 통계 데이터를 불러옵니다.
        fetchYearlyData(); // 1년 간의 월별 데이터를 불러옵니다.
    }, [selectedMonth]); // selectedMonth가 변경될 때마다 호출

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
    const monthlyTrendData = statistics?.monthlyTrend || []; // 월별 스케줄 추이 데이터

    // 데이터가 비어있다면, 차트가 렌더링되지 않도록 조건 추가
    if (monthlyTrendData.length === 0) {
        return <p>월별 데이터가 없습니다.</p>;
    }

    // "2025-09-01T00:00:00" -> "2025-09" 형식으로 변환
    const transformedData = monthlyTrendData.map(item => ({
        month: item.month.split('T')[0].slice(0, 7), // "2025-09" 형식으로 변경
        scheduleCount: item.scheduleCount
    }));

    return (
        <div>
            <h3>월별 스케줄 추이</h3>
            <ResponsiveContainer width="100%" height={300}>
                <LineChart data={transformedData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" /> {/* x축에 "2025-09", "2025-10" 형식으로 표시 */}
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

            <div>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
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
                        {statistics?.peopleSummary?.restOfTheList?.map((person, index) => (
                            <tr key={index} style={{ backgroundColor: index % 2 === 0 ? '#f9f9f9' : '#fff' }}>
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

            <div>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
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
                        {statistics?.placeSummary?.restOfTheList?.map((place, index) => (
                            <tr key={index} style={{ backgroundColor: index % 2 === 0 ? '#f9f9f9' : '#fff' }}>
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

    if (loading) {
        return <p>Loading...</p>;
    }

    return (
        <div>
            <h1>통계</h1>
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

            {selectedTab === 'people' && renderPeopleSummary()}
            {selectedTab === 'place' && renderPlaceSummary()}
            {selectedTab === 'monthly' && renderMonthlyTrend()}
            {selectedTab === 'weekday' && renderWeekdayDistribution()}
            {selectedTab === 'hour' && renderHourlyDistribution()}
        </div>
    );
};
