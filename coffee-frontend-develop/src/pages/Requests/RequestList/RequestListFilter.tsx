import { Controller, useFormContext } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Checkbox, FormControlLabel, FormGroup, Stack, Typography } from '@mui/material';

import { getCookies } from '@/apis/AppUser/cookie';
import { showToast } from '@/utils/showToast';

const RequestListFilter = () => {
    const { t } = useTranslation();
    const { control, watch } = useFormContext();
    const role = getCookies('role');

    const preventUncheckLast =
        (
            groupKey: 'serviceTypes' | 'statuses',
            _fieldKey: string,
            fieldOnChange: (v: boolean) => void
        ) =>
        (_e: React.ChangeEvent<HTMLInputElement>, checked: boolean) => {
            // checked === false : 지금 끄려는 상황
            if (!checked) {
                const group = watch(groupKey) ?? {};
                const checkedCount = Object.values(group).filter(Boolean).length;

                // 마지막 1개를 끄려는 순간이면 무시
                if (checkedCount <= 1) {
                    showToast.error(
                        t(
                            `최소 1개의 ${groupKey === 'serviceTypes' ? '서비스 유형' : '요청 상태'}을 선택해주세요.`
                        )
                    );
                    return;
                }
            }

            fieldOnChange(checked);
        };

    return (
        <Stack sx={{ gap: '16px' }}>
            <Stack sx={{ flexDirection: 'row', alignItems: 'center', gap: '40px' }}>
                <Typography variant="body1/semibold" sx={{ flexShrink: '0' }}>
                    {t('요청 분류')}
                </Typography>
                <FormGroup sx={{ flexDirection: 'row', gap: '8px' }}>
                    <Controller
                        name="serviceTypes.VILLAGE_HEAD"
                        control={control}
                        render={({ field }) => (
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        {...field}
                                        checked={field.value}
                                        onChange={preventUncheckLast(
                                            'serviceTypes',
                                            'VILLAGE_HEAD',
                                            field.onChange
                                        )}
                                    />
                                }
                                label={<Typography fontSize={14}>{t('면장 관리')}</Typography>}
                            />
                        )}
                    />
                    <Controller
                        name="serviceTypes.FARMER"
                        control={control}
                        render={({ field }) => (
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        {...field}
                                        checked={field.value}
                                        onChange={preventUncheckLast(
                                            'serviceTypes',
                                            'FARMER',
                                            field.onChange
                                        )}
                                    />
                                }
                                label={<Typography fontSize={14}>{t('농부 관리')}</Typography>}
                            />
                        )}
                    />
                    {
                        // 부관리자 - 농림부의 경우 수매 관리에 대한 요청 확인 불가능
                        role !== 'VICE_ADMIN_AGRICULTURE_MINISTRY_OFFICER' && (
                            <Controller
                                name="serviceTypes.PURCHASE"
                                control={control}
                                render={({ field }) => (
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                {...field}
                                                checked={field.value}
                                                onChange={preventUncheckLast(
                                                    'serviceTypes',
                                                    'PURCHASE',
                                                    field.onChange
                                                )}
                                            />
                                        }
                                        sx={{ fontSize: '14px' }}
                                        label={
                                            <Typography fontSize={14}>{t('수매 관리')}</Typography>
                                        }
                                    />
                                )}
                            />
                        )
                    }
                    <Controller
                        name="serviceTypes.SECTION"
                        control={control}
                        render={({ field }) => (
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        {...field}
                                        checked={field.value}
                                        onChange={preventUncheckLast(
                                            'serviceTypes',
                                            'SECTION',
                                            field.onChange
                                        )}
                                    />
                                }
                                label={<Typography fontSize={14}>{t('지역 관리')}</Typography>}
                            />
                        )}
                    />
                </FormGroup>
            </Stack>
            <Stack sx={{ flexDirection: 'row', alignItems: 'center', gap: '40px' }}>
                <Typography variant="body1/semibold" sx={{ flexShrink: '0' }}>
                    {t('요청 상태')}
                </Typography>
                <FormGroup sx={{ flexDirection: 'row', gap: '8px' }}>
                    <Controller
                        name="statuses.PENDING"
                        control={control}
                        render={({ field }) => (
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        {...field}
                                        checked={field.value}
                                        onChange={preventUncheckLast(
                                            'statuses',
                                            'PENDING',
                                            field.onChange
                                        )}
                                    />
                                }
                                sx={{ fontSize: '14px' }}
                                label={<Typography fontSize={14}>{t('대기')}</Typography>}
                            />
                        )}
                    />
                    <Controller
                        name="statuses.APPROVED"
                        control={control}
                        render={({ field }) => (
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        {...field}
                                        checked={field.value}
                                        onChange={preventUncheckLast(
                                            'statuses',
                                            'APPROVED',
                                            field.onChange
                                        )}
                                    />
                                }
                                label={<Typography fontSize={14}>{t('승인')}</Typography>}
                            />
                        )}
                    />
                    <Controller
                        name="statuses.REJECTED"
                        control={control}
                        render={({ field }) => (
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        {...field}
                                        checked={field.value}
                                        onChange={preventUncheckLast(
                                            'statuses',
                                            'REJECTED',
                                            field.onChange
                                        )}
                                    />
                                }
                                label={<Typography fontSize={14}>{t('거절')}</Typography>}
                            />
                        )}
                    />
                </FormGroup>
            </Stack>
        </Stack>
    );
};

export default RequestListFilter;
