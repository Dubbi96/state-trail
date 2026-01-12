import { useEffect } from 'react';

import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';

import { Close } from '@mui/icons-material';
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Stack,
} from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';

import { useGetVillageHeadList } from '@/apis/AppUser/useGetVillageHeadList';
import {
    UpdateApprovalFarmerReq,
    useUpdateApprovalFarmer,
} from '@/apis/Approval/useUpdateApprovalFarmer';
import { useGetFarmerDetail } from '@/apis/Farmer/useGetFarmerDetail';
import { QUERY_KEYS } from '@/apis/QueryKeys';
import AddPhoto from '@/components/AddPhoto';
import LabelAndInput from '@/components/LabelAndInput';
import LabelAndSelect from '@/components/LabelAndSelect';
import LabelComponentsLayout from '@/components/LabelComponentsLayout';
import { useDialog } from '@/hooks/useDialog';

type FarmerEditDialogProps = {
    open: boolean;
    onClose: () => void;
    farmerId: number;
};

export const FarmerEditDialog = ({ open, onClose, farmerId }: FarmerEditDialogProps) => {
    const { t } = useTranslation();
    const queryClient = useQueryClient();
    const { openDialog } = useDialog();
    const { data: villageHeadList } = useGetVillageHeadList();
    const { data: farmer, isLoading: farmerDetailLoading } = useGetFarmerDetail(String(farmerId));

    const { reset, control, register, watch, setValue, handleSubmit } =
        useForm<UpdateApprovalFarmerReq>({
            defaultValues: {
                villageHeadId: farmer?.villageHeadId || null,
                name: farmer?.farmerName || '',
                identificationPhoto: undefined,
                farmerId,
            },
        });

    const { mutateAsync: updateApprovalFarmerMutateAsync } = useUpdateApprovalFarmer();
    const onSubmit = (data: UpdateApprovalFarmerReq) => {
        updateApprovalFarmerMutateAsync({
            ...data,
        })
            .then((res) => {
                if (res.data.code === 'SUCCESS') {
                    queryClient.invalidateQueries({
                        queryKey: QUERY_KEYS.FARMER.getFarmerList(),
                    });
                    openDialog({
                        title: t('농부 수정 요청 성공'),
                        description: t('관리자가 요청 승인 후 목록에서 확인가능합니다.'),
                        variant: 'confirm',
                        primaryAction: {
                            name: t('확인'),
                            onClick: () => {
                                reset();
                            },
                        },
                    });
                    return;
                }
                openDialog({
                    title: t('농부 수정 요청 실패'),
                    description: t('권한 확인 또는 관리자에게 문의해주세요.'),
                    variant: 'alert',
                    primaryAction: {
                        name: t('확인'),
                        onClick: () => {},
                    },
                });
            })
            .catch((err) => {
                openDialog({
                    title: t('농부 수정 요청 실패'),
                    description: `${t('에러')} : ${err}.\n 관리자에게 문의 바랍니다.`,
                    variant: 'alert',
                    primaryAction: {
                        name: t('확인'),
                        onClick: () => {
                            reset();
                        },
                    },
                });
            });
    };

    const handleClose = () => {
        onClose();
        reset();
    };

    useEffect(() => {
        if (farmer) {
            reset({
                villageHeadId: farmer.villageHeadId,
                name: farmer.farmerName,
                farmerId: farmerId,
            });
        }
    }, [farmer, reset]);

    if (farmerDetailLoading || !farmer) return null;
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{t('농부 수정')}</DialogTitle>
            <IconButton
                aria-label="close"
                onClick={handleClose}
                sx={(theme) => ({
                    position: 'absolute',
                    right: 8,
                    top: 8,
                    color: theme.palette.grey[500],
                })}
            >
                <Close />
            </IconButton>

            <DialogContent>
                <Stack gap={'12px'}>
                    <LabelAndSelect
                        labelValue={t('면장')}
                        control={control}
                        fieldName="villageHeadId"
                        placeholder={t('면장 선택')}
                        selectArr={
                            villageHeadList?.map((head) => {
                                return { value: String(head.id), label: head.appUserName };
                            }) || []
                        }
                    />
                    <LabelAndInput
                        labelValue={t('이름')}
                        placeholder={t('이름')}
                        register={register}
                        fieldName="name"
                    />
                    <LabelComponentsLayout labelValue={t('사진')}>
                        <AddPhoto
                            fieldName="identificationPhoto"
                            watch={watch}
                            setValue={setValue}
                            currentUrl={farmer?.identificationPhotoUrl}
                        />
                    </LabelComponentsLayout>
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button onClick={handleClose} sx={{ flex: 1 }} variant="containedGrey">
                    취소
                </Button>
                <Button
                    onClick={handleSubmit(onSubmit)}
                    form="create-farmer-form"
                    variant="containedBlue"
                    sx={{ flex: 1 }}
                >
                    {t('확인')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
