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
import dayjs, { Dayjs } from 'dayjs';

import { useGetVillageHeadList } from '@/apis/AppUser/useGetVillageHeadList';
import {
    CreateApprovalPurchaseReq,
    useCreateApprovalPurchase,
} from '@/apis/Approval/useCreateApprovalPurchase';
import CustomDatePicker from '@/components/CustomDatePicker';
import LabelAndInput from '@/components/LabelAndInput';
import LabelAndSelect from '@/components/LabelAndSelect';
import LabelComponentsLayout from '@/components/LabelComponentsLayout';
import TextArea from '@/components/TextArea';
import { useDialog } from '@/hooks/useDialog';

type TreesPurchaseCreateDialogProps = {
    open: boolean;
    onClose: () => void;
};

export const TreesPurchaseCreateDialog = ({ open, onClose }: TreesPurchaseCreateDialogProps) => {
    const { t } = useTranslation();
    const { openDialog } = useDialog();
    const { mutateAsync: createPurchase } = useCreateApprovalPurchase();

    const { data: villageHeadList } = useGetVillageHeadList(); // 면장 목록

    const methods = useForm<CreateApprovalPurchaseReq>({
        defaultValues: { purchaseDate: dayjs(new Date()).format('YYYY-MM-DD') },
    });

    const onSubmit = (data: CreateApprovalPurchaseReq) => {
        createPurchase(data)
            .then((res) => {
                if (res?.data?.code === 'SUCCESS') {
                    openDialog({
                        title: t('수매 내역 등록 요청 성공'),
                        description: t('관리자가 요청 승인 후 목록에서 확인 가능합니다.'),
                        variant: 'confirm',
                        primaryAction: {
                            name: t('확인'),
                            onClick: () => {
                                methods.reset();
                            },
                        },
                    });

                    handleClose();
                    return;
                } else
                    openDialog({
                        title: t('수매 내역 등록 요청 실패'),
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
                    title: t('수매 내역 등록 요청 실패'),
                    description: `${t('에러')} : ${err}.\n 관리자에게 문의 바랍니다.`,
                    variant: 'alert',
                    primaryAction: {
                        name: t('확인'),
                        onClick: () => {
                            methods.reset();
                        },
                    },
                });
            });
    };

    const handleClose = () => {
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{t('수매 내역 등록')}</DialogTitle>
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
                        control={methods.control}
                        fieldName="villageHeadId"
                        placeholder={t('면장 선택')}
                        selectArr={
                            villageHeadList?.map((head) => {
                                return { value: String(head.id), label: head.appUserName };
                            }) || []
                        }
                    />
                    <LabelComponentsLayout labelValue={t('거래일자')}>
                        <CustomDatePicker
                            value={dayjs(methods.watch('purchaseDate'))}
                            onChange={(newValue: Dayjs | null) =>
                                methods.setValue(
                                    'purchaseDate',
                                    newValue?.format('YYYY-MM-DD').toString() || ''
                                )
                            }
                        />
                    </LabelComponentsLayout>
                    <LabelAndInput
                        labelValue={t('수량')}
                        fieldName="quantity"
                        register={methods.register}
                        placeholder={t('수량을 입력해주세요.')}
                    />
                    <LabelAndInput
                        labelValue={t('단가')}
                        fieldName="unitPrice"
                        register={methods.register}
                        placeholder={t('단가를 입력해주세요.')}
                    />
                    <LabelAndInput
                        labelValue={t('총액')}
                        fieldName="totalPrice"
                        register={methods.register}
                        placeholder={t('총액을 입력해주세요.')}
                    />
                    <LabelAndInput
                        labelValue={t('차감액')}
                        fieldName="deduction"
                        register={methods.register}
                        placeholder={t('차감액을 입력해주세요.')}
                    />
                    <LabelAndInput
                        labelValue={t('지급액')}
                        fieldName="paymentAmount"
                        register={methods.register}
                        placeholder={t('지급액을 입력해주세요.')}
                    />
                    <LabelComponentsLayout labelValue="비고">
                        <TextArea
                            fieldName="remarks"
                            register={methods.register}
                            placeholder={'텍스트를 작성하시오.'}
                        />
                    </LabelComponentsLayout>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} sx={{ flex: 1 }} variant="containedGrey">
                    취소
                </Button>
                <Button
                    onClick={methods.handleSubmit(onSubmit)}
                    form="create-purchase-form"
                    variant="containedBlue"
                    sx={{ flex: 1 }}
                >
                    {t('확인')}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
