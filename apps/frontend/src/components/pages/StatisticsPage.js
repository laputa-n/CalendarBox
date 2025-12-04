import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Legend } from 'recharts';
import { ApiService } from '../../services/apiService';

export const StatisticsPage = () => {
    const [statistics, setStatistics] = useState(null);
    const [loading, setLoading] = useState(false);
    const [selectedTab, setSelectedTab] = useState('people');
    const [selectedWeekday, setSelectedWeekday] = useState('1'); // 월요일 기본값
    const [selectedMonth, setSelectedMonth] = useState(getCurrentMonth()); // 기본값은 이번 달
    const [yearlyData, setYearlyData] = useState([]); // 1년 동안의 월별 데이터를 저장

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
            const peopleSummaryResponse = await ApiService.getPeopleSummary(selectedMonth);
            const placeSummaryResponse = await ApiService.getPlaceSummary(selectedMonth);
            const scheduleDayHourResponse = await ApiService.getScheduleDayHourDistribution();
            const monthlyTrendResponse = await ApiService.getMonthlyScheduleTrend(selectedMonth);

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
    const renderMonthlyTrend = () => (
        <div>
            <h3>월별 스케줄 추이</h3>
            <ResponsiveContainer width="100%" height={300}>
                <LineChart data={yearlyData}>
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

    // 요일별 스케줄 (원형 그래프)
    const renderWeekdayDistribution = () => (
        <div>
            <h3>요일별 스케줄</h3>
            <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                    <Pie
                        data={statistics?.scheduleDayHour || []}
                        dataKey="scheduleCount"
                        nameKey="dayOfWeek"
                        cx="50%"
                        cy="50%"
                        outerRadius={100}
                        fill="#82ca9d"
                    />
                    <Tooltip />
                    <Legend />
                </PieChart>
            </ResponsiveContainer>
        </div>
    );

    // 시간대별 스케줄 (막대 그래프, 3시간 간격)
    const renderHourlyDistribution = () => (
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
                        <option key={index} value={index + 1}>
                            {day}
                        </option>
                    ))}
                </select>
            </div>
            <ResponsiveContainer width="100%" height={300}>
                <BarChart data={statistics?.scheduleDayHour || []}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="hourOfDay" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="scheduleCount" fill="#8884d8" />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );

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
