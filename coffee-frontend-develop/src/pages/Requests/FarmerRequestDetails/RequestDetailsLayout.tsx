import React, { useState } from 'react';

import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router';

import { Button, Chip, Divider, Stack, Typography } from '@mui/material';
import dayjs from 'dayjs';

import { getCookies } from '@/apis/AppUser/cookie';
import { useApproveApproval } from '@/apis/Approval/useApproveApproval';
import { useDeleteApproval } from '@/apis/Approval/useDeleteApproval';
import { useGetApprovalDetails } from '@/apis/Approval/useGetApprovalDetails';
import { useRejectApproval } from '@/apis/Approval/useRejectApproval';
import TextArea from '@/components/TextArea';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';
import { palette } from '@/themes';
import { TChipColor } from '@/typings/Chip';
import { showToast } from '@/utils/showToast';

import { REQUEST_METHOD, REQUEST_SERVICE_TYPE, REQUEST_STATUS } from '../constants';

type RequestDetailsLayoutProps = {
    children: React.ReactNode;
};

type TRequestDetailInput = {
    rejectedReason: string;
};

const RequestDetailsLayout = ({ children }: RequestDetailsLayoutProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { watch, register, reset } = useForm<TRequestDetailInput>();
    const [showTextArea, setShowTextArea] = useState(false);
    const role = getCookies('role');

    const { id } = useParams();
    const { openDialog } = useDialog();

    const { data: details } = useGetApprovalDetails(id);
    const { mutateAsync: rejectApproval } = useRejectApproval();
    const { mutateAsync: approveApproval } = useApproveApproval();
    const { mutateAsync: deleteApproval } = useDeleteApproval();

    const toggleShowTextArea = () => {
        setShowTextArea((prev) => !prev);
    };

    if (!id) return null;

    const handleApproveRequest = () => {
        openDialog({
            title: t('요청 승인'),
            description: t('요청을 승인하시겠습니까?'),
            variant: 'confirm',
            primaryAction: {
                name: t('확인'),
                onClick: async () => {
                    await approveApproval(id).then((res) => {
                        if (res.code === 'SUCCESS') showToast.success(t('승인 처리되었습니다.'));
                        else showToast.error(res.message);
                    });
                },
            },
            secondaryAction: {
                name: t('취소'),
                onClick: () => {},
            },
        });
    };

    const handleRejectRequest = () => {
        if (watch('rejectedReason') === '') {
            showToast.error(t('거절 사유를 입력해주세요.'));
            return;
        }

        openDialog({
            title: t('승인 요청 거절'),
            description: t('거절하시겠습니까?'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: async () => {
                    await rejectApproval({
                        approvalId: id,
                        rejectedReason: watch('rejectedReason'),
                    })
                        .then((res) => {
                            if (res.code === 'SUCCESS') {
                                showToast.success('거절 처리되었습니다.');
                            }
                        })
                        .catch(() => showToast.error(t('요청에 실패했습니다.')));
                },
            },
            secondaryAction: {
                name: t('취소'),
                onClick: () => {},
            },
        });
    };

    const handleDeleteApproval = () => {
        openDialog({
            title: t('요청 삭제'),
            description: t('삭제된 내용은 되돌릴 수 없습니다.'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: async () => {
                    await deleteApproval({
                        approvalId: Number(id),
                    })
                        .then((res) => {
                            if (res.code === 'SUCCESS') {
                                showToast.success('삭제 되었습니다.');
                                navigate('/requests');
                            }
                        })
                        .catch(() => showToast.error(t('삭제에 실패했습니다. 다시 시도해주세요.')));
                },
            },
            secondaryAction: {
                name: t('취소'),
                onClick: () => {},
            },
        });
    };

    if (!details) return null;
    return (
        <Stack sx={{ width: '100%' }}>
            <Title title={t('요청 상세')}>
                {details.status === 'PENDING' && (
                    <Stack
                        sx={{
                            'margin': 'auto',
                            'flexDirection': 'row',
                            'gap': '10px',
                            '& .MuiButton-root': { height: '40px' },
                            'padding': '0 0 30px 0',
                        }}
                    >
                        <Button variant="outlinedRed" onClick={handleDeleteApproval}>
                            삭제
                        </Button>
                    </Stack>
                )}
            </Title>
            {/* 요청 기본 정보 */}
            <Stack sx={{ padding: '0 32px', gap: '30px' }}>
                <Stack
                    sx={{
                        'padding': '32px 24px',
                        'flexDirection': 'row',
                        'gap': '32px',
                        'justifyContent': 'space-between',
                        'border': `1px solid ${palette.grey[50]}`,
                        '& > p': {
                            fontSize: '14px',
                        },
                        '& .MuiStack-root': {
                            flexDirection: 'row',
                            flex: 1,
                            justifyContent: 'space-between',
                            alignItems: 'cente r',
                        },
                    }}
                >
                    <Stack>
                        <Typography sx={{ fontWeight: 'bold', color: palette.grey[900] }}>
                            {t('요청 일시')}
                        </Typography>
                        <Typography>
                            {dayjs(details.createdAt).format('YYYY-MM-DD hh:mm')}
                        </Typography>
                    </Stack>
                    <Divider orientation="vertical" flexItem />
                    <Stack>
                        <Typography sx={{ fontWeight: 'bold', color: palette.grey[900] }}>
                            {t('요청 분류')}
                        </Typography>
                        <Typography>{`${t(REQUEST_SERVICE_TYPE[details.serviceType])} / ${t(REQUEST_METHOD[details.method])}`}</Typography>
                    </Stack>
                    <Divider orientation="vertical" flexItem />
                    <Stack>
                        <Typography sx={{ fontWeight: 'bold', color: palette.grey[900] }}>
                            {t('담당자')}
                        </Typography>
                        <Typography>{details.requesterName}</Typography>
                    </Stack>
                    <Divider orientation="vertical" flexItem />
                    <Stack>
                        <Typography sx={{ fontWeight: 'bold', color: palette.grey[900] }}>
                            {t('요청 상태')}
                        </Typography>
                        <Chip
                            color={REQUEST_STATUS[details.status].color as TChipColor}
                            label={REQUEST_STATUS[details.status].label}
                        />
                    </Stack>
                </Stack>
                {/* 거절 사유 */}
                {details?.status === 'REJECTED' && (
                    <>
                        <Typography variant="title/semibold">{t('거절 사유')}</Typography>
                        <Stack
                            sx={{
                                border: `1px solid ${palette.grey[50]}`,
                                padding: '16px',
                                minHeight: '150px',
                            }}
                        >
                            <Typography sx={{ color: palette.grey[700] }}>
                                {details.rejectedReason}
                            </Typography>
                        </Stack>
                    </>
                )}
                {/* 요청 상세 정보 */}
                {children}
                {/* 승인, 거절 버튼 - PENDING*/}
                {role === 'ADMIN' && details.status === 'PENDING' && !showTextArea && (
                    <Stack
                        sx={{
                            'margin': 'auto',
                            'flexDirection': 'row',
                            'gap': '10px',
                            '& .MuiButton-root': { height: '40px' },
                            'padding': '0 0 30px 0',
                        }}
                    >
                        <Button variant="containedRed" onClick={toggleShowTextArea}>
                            {t('거절')}
                        </Button>
                        <Button variant="containedGrey" onClick={handleApproveRequest}>
                            {t('승인')}
                        </Button>
                    </Stack>
                )}
                {/* 거절 사유 입력 form */}
                {showTextArea && (
                    <Stack sx={{ gap: '15px' }}>
                        <Typography variant="title/semibold">{t('거절 사유 입력')}</Typography>
                        <TextArea
                            register={register}
                            fieldName="rejectedReason"
                            placeholder={t('거절 사유 입력')}
                        />
                        <Stack
                            sx={{
                                'justifyContent': 'flex-end',
                                'flexDirection': 'row',
                                'gap': '10px',
                                'padding': '0 0 30px 0',
                                '& .MuiButton-root': { height: '40px' },
                            }}
                        >
                            <Button
                                variant="containedGrey"
                                onClick={() => {
                                    toggleShowTextArea();
                                    reset();
                                }}
                            >
                                {t('취소')}
                            </Button>
                            <Button variant="containedRed" onClick={handleRejectRequest}>
                                {t('완료')}
                            </Button>
                        </Stack>
                    </Stack>
                )}
            </Stack>
        </Stack>
    );
};

export default RequestDetailsLayout;
