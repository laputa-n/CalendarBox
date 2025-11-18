import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, Legend } from 'recharts';
import { ApiService } from '../../services/apiService';

export const StatisticsPage = () => {
    const [statistics, setStatistics] = useState(null);
    const [loading, setLoading] = useState(false);
    const [selectedPeriod, setSelectedPeriod] = useState('month');

    useEffect(() => {
        fetchStatistics();
    }, [selectedPeriod]);

    const fetchStatistics = async () => {
        try {
            setLoading(true);

            // API 요청 부분
            const peopleSummaryResponse = await ApiService.getPeopleSummary(selectedPeriod); // 사람 통계 요약
            const placeSummaryResponse = await ApiService.getPlaceSummary(selectedPeriod);   // 장소 통계 요약
            const scheduleDayHourResponse = await ApiService.getScheduleDayHourDistribution(); // 요일-시간대별 스케줄 분포
            const monthlyTrendResponse = await ApiService.getMonthlyScheduleTrend();         // 월별 스케줄 추이

            setStatistics({
                ...peopleSummaryResponse.data,
                ...placeSummaryResponse.data,
                scheduleDayHour: scheduleDayHourResponse.data,
                monthlyTrend: monthlyTrendResponse.data,
            });

        } catch (error) {
            console.error('Failed to fetch statistics:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading && !statistics) {
        return (
            <div style={{ textAlign: 'center', padding: '3rem' }}>
                <p style={{ color: '#6b7280' }}>통계 데이터를 불러오는 중...</p>
            </div>
        );
    }

    // 장소 통계 차트 (Top 3)
    const renderPlaceSummaryChart = () => (
        <ResponsiveContainer width="100%" height={300}>
            <BarChart data={statistics?.top3 || []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="placeName" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="visitCount" fill="#82ca9d" name="방문 횟수" />
                <Bar dataKey="totalStayMin" fill="#8884d8" name="총 머문 시간" />
                <Bar dataKey="avgStayMin" fill="#ffc658" name="평균 머문 시간" />
            </BarChart>
        </ResponsiveContainer>
    );

    // 월별 스케줄 추이 차트
    const renderMonthlyTrendChart = () => (
        <ResponsiveContainer width="100%" height={300}>
            <LineChart data={statistics?.monthlyTrend || []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="scheduleCount" stroke="#8884d8" />
            </LineChart>
        </ResponsiveContainer>
    );

    // 요일-시간대별 스케줄 분포 차트
    const renderScheduleDayHourChart = () => (
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
    );

    // 요일-시간대별 스케줄 분포 차트
    const renderPeopleSummaryChart = () => (
        <ResponsiveContainer width="100%" height={300}>
            <BarChart data={statistics?.peopleSummary || []}> {/* peopleSummary 데이터 사용 */}
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="scheduleCount" fill="#82ca9d" name="일정 수" />
                <Bar dataKey="totalAmount" fill="#8884d8" name="총 지출 금액" />
                <Bar dataKey="avgAmount" fill="#ffc658" name="평균 지출 금액" />
            </BarChart>
        </ResponsiveContainer>
    );

    return (
        <div>
            <div>
                <h1>통계</h1>
                <select value={selectedPeriod} onChange={(e) => setSelectedPeriod(e.target.value)}>
                    <option value="week">이번 주</option>
                    <option value="month">이번 달</option>
                    <option value="quarter">이번 분기</option>
                    <option value="year">올해</option>
                </select>
            </div>

            {/* 월별 스케줄 추이 차트 */}
            <div style={{ marginBottom: '2rem' }}>
                <h2>월별 스케줄 추이</h2>
                {renderMonthlyTrendChart()}
            </div>

            {/* 요일-시간대별 스케줄 분포 차트 */}
            <div style={{ marginBottom: '2rem' }}>
                <h2>요일-시간대별 스케줄 분포</h2>
                {renderScheduleDayHourChart()}
            </div>

            {/* 장소 통계 차트 (Top 3) */}
            <div style={{ marginBottom: '2rem' }}>
                <h2>장소 통계 요약 (Top 3)</h2>
                {renderPlaceSummaryChart()}
            </div>

            {/* 사람 통계 요약 차트 */}
            <div style={{ marginBottom: '2rem' }}>
                <h2>사람 통계 요약 (Top 3)</h2>
                {renderPeopleSummaryChart()}
            </div>
        </div>
    );
};
