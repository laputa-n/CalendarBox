import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Legend } from 'recharts';
import { ApiService } from '../../services/apiService';

export const StatisticsPage = () => {
    const [statistics, setStatistics] = useState(null);
    const [loading, setLoading] = useState(false);
    const [selectedTab, setSelectedTab] = useState('people'); // 'people' 기본값 설정

    useEffect(() => {
        fetchStatistics();
    }, [selectedTab]);

   const fetchStatistics = async () => {
    try {
        setLoading(true);
        console.log('Fetching statistics...');
        // API 요청 (month 파라미터 수정)
        const peopleSummaryResponse = await ApiService.getPeopleSummary('2025-11');  // month 파라미터 수정
        console.log('People summary:', peopleSummaryResponse);
        const placeSummaryResponse = await ApiService.getPlaceSummary('2025-11');
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
    // 스타일링: 탭 버튼
    const tabButtonStyle = {
        padding: '12px 24px',
        fontSize: '16px',
        margin: '0 10px',
        background: 'linear-gradient(135deg, #6b8df2, #85c4ff)', // 그라데이션 배경
        color: '#fff',
        border: 'none',
        borderRadius: '20px', // 모서리 둥글게
        cursor: 'pointer',
        transition: 'all 0.3s ease', // 호버 효과
    };

    const tabButtonActiveStyle = {
        ...tabButtonStyle,
        background: 'linear-gradient(135deg, #85c4ff, #6b8df2)', // 활성화된 탭
        transform: 'scale(1.05)', // 클릭된 탭 강조
    };

    // 탭 버튼 클릭 스타일
    const handleTabClick = (tab) => {
        setSelectedTab(tab);
    };

    // 사람 통계 (Top 3 + 나머지 목록)
    const renderPeopleSummary = () => (
        <div>
            <h3>사람 통계</h3>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2rem' }}>
                {/* Top 3 카드 */}
                {statistics?.peopleSummary?.top3?.map((person, index) => (
                    <div
                        key={index}
                        style={{
                            width: '30%',
                            padding: '1rem',
                            borderRadius: '8px',
                            backgroundColor: '#ADD8E6', // 배경색
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

            {/* 나머지 리스트 (표 형식) */}
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
                {/* Top 3 카드 */}
                {statistics?.placeSummary?.top3?.map((place, index) => (
                    <div
                        key={index}
                        style={{
                            width: '30%',
                            padding: '1rem',
                            borderRadius: '8px',
                            backgroundColor: '#ADD8E6', // 배경색
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

            {/* 나머지 장소 리스트 (표 형식) */}
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

    // 시간대별 스케줄 차트
    const renderScheduleTimeChart = () => (
        <div>
            <h3>시간대별 스케줄</h3>
            <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                    <Pie
                        data={statistics?.scheduleDayHour || []}
                        dataKey="scheduleCount"
                        nameKey="hourOfDay"
                        cx="50%"
                        cy="50%"
                        outerRadius={100}
                        fill="#8884d8"
                    />
                    <Tooltip />
                    <Legend />
                </PieChart>
            </ResponsiveContainer>
        </div>
    );

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
                    style={selectedTab === 'time' ? tabButtonActiveStyle : tabButtonStyle}
                    onClick={() => setSelectedTab('time')}
                >
                    시간 스케줄
                </button>
            </div>

            {selectedTab === 'people' && renderPeopleSummary()}
            {selectedTab === 'place' && renderPlaceSummary()}
            {selectedTab === 'time' && renderScheduleTimeChart()}
        </div>
    );
};
