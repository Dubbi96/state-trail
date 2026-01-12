import { MouseEvent, useState } from 'react';

import { useTranslation } from 'react-i18next';

import { MoreHoriz } from '@mui/icons-material';
import {
    Button,
    Divider,
    IconButton,
    Menu,
    MenuItem,
    Select,
    Stack,
    TableCell,
    TableRow,
    Typography,
} from '@mui/material';
import { Plus } from '@phosphor-icons/react';
import dayjs, { Dayjs } from 'dayjs';

import { getCookies } from '@/apis/AppUser/cookie';
import { useGetVillageHeadList } from '@/apis/AppUser/useGetVillageHeadList';
import { useDeletePurchase } from '@/apis/Approval/useDeletePurchase';
import { GetPurchaseList, useGetPurchaseList } from '@/apis/Purchase/getPurchaseList';
import CustomDatePicker from '@/components/CustomDatePicker';
import PageLayout from '@/components/PageLayout';
import Table from '@/components/Table';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';
import { palette } from '@/themes';
import { TPurchase } from '@/typings/Purchase';

import { TreesPurchaseCreateDialog } from './create-dialog';
import { TreesPurchaseEditDialog } from './edit-dialog';

const TreesPurchaseList = () => {
    const { openDialog } = useDialog();
    const { t } = useTranslation();
    const { mutate: deletePurchase } = useDeletePurchase();
    const [openCreateDialog, setOpenCreateDialog] = useState(false);
    const [openEditDialog, setOpenEditDialog] = useState(false);
    const [selectedPurchaseDetail, setSelectedPurchaseDetail] = useState<TPurchase | null>(null);

    const { data: villageHeadList } = useGetVillageHeadList(); // 면장 목록
    const [villageHeadId, setVillageHeadId] = useState<string | undefined>();
    const today = dayjs();
    const [startDate, setStartDate] = useState<Dayjs | undefined>(today.subtract(1, 'month'));
    const [endDate, setEndDate] = useState<Dayjs | undefined>(today);

    const { data: purchaseList } = useGetPurchaseList({
        startDate,
        endDate,
        villageHeadId,
    });

    const [menuState, setMenuState] = useState<{
        anchorEl: HTMLElement | null;
        rowId: number | null;
    }>({ anchorEl: null, rowId: null });

    const open = Boolean(menuState.anchorEl);

    const handleClickMenu = (rowId: number) => (e: MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        setMenuState({ anchorEl: e.currentTarget, rowId });
    };

    const handleCloseMenu = () => setMenuState({ anchorEl: null, rowId: null });

    const handleClickDelete = (id: number) => {
        openDialog({
            title: t('해당 구매 내역을 삭제하시겠습니까?'),
            description: t('삭제하면 복구가 불가능합니다.'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: () => {
                    deletePurchase({ purchaseId: id });
                },
            },
            secondaryAction: { name: t('취소'), onClick: () => {} },
        });
    };

    const role = getCookies('role');

    const renderRow = (row: GetPurchaseList) => {
        return (
            <TableRow key={row.id}>
                <TableCell>{row.id.toString()}</TableCell>
                <TableCell>{row.managerName}</TableCell>
                <TableCell>{row.villageHeadName}</TableCell>
                <TableCell>{dayjs(row.purchaseDate).format('YYYY-MM-DD')}</TableCell>
                <TableCell>{row.quantity.toLocaleString()}</TableCell>
                <TableCell>{row.unitPrice.toLocaleString()}원</TableCell>
                <TableCell>{row.totalPrice.toLocaleString()}원</TableCell>
                <TableCell>{row.deduction.toLocaleString()}원</TableCell>
                <TableCell>{row.paymentAmount.toLocaleString()}원</TableCell>
                <TableCell>{row?.remarks}</TableCell>
                <TableCell onClick={(e) => e.stopPropagation()} align="right">
                    <IconButton
                        id={`basic-button-${row.id}`}
                        size="small"
                        aria-controls={open ? 'basic-menu' : undefined}
                        aria-haspopup="true"
                        aria-expanded={open ? 'true' : undefined}
                        onClick={handleClickMenu(row.id)}
                    >
                        <MoreHoriz />
                    </IconButton>
                    <Menu
                        anchorEl={menuState.anchorEl}
                        open={open && menuState.rowId === row.id}
                        onClose={handleCloseMenu}
                        slotProps={{
                            list: {
                                'aria-labelledby': 'basic-button',
                            },
                        }}
                    >
                        <MenuItem
                            onClick={() => {
                                setOpenEditDialog(true);
                                setSelectedPurchaseDetail(row);
                                handleCloseMenu();
                            }}
                        >
                            수정하기
                        </MenuItem>
                        {role !== 'VILLAGE_HEAD' && <Divider />}
                        {role !== 'VILLAGE_HEAD' && (
                            <MenuItem
                                onClick={() => {
                                    handleClickDelete(row.id);
                                    handleCloseMenu();
                                }}
                            >
                                삭제하기
                            </MenuItem>
                        )}
                    </Menu>
                </TableCell>
            </TableRow>
        );
    };

    const totalData = purchaseList?.reduce(
        (acc, curr) => {
            return {
                quantity: acc.quantity + curr.quantity,
                unitPrice: acc.unitPrice + curr.unitPrice,
                totalPrice: acc.totalPrice + curr.totalPrice,
                deduction: acc.deduction + curr.deduction,
                paymentAmount: acc.paymentAmount + curr.paymentAmount,
            };
        },
        { quantity: 0, unitPrice: 0, totalPrice: 0, deduction: 0, paymentAmount: 0 }
    );

    const totalRow = () => {
        if (!totalData) return null;
        return (
            <TableRow sx={{ '&:hover': { backgroundColor: 'transparent' } }}>
                <TableCell
                    colSpan={4}
                    align="center"
                    sx={{ fontWeight: 600, backgroundColor: palette.grey[50] }}
                >
                    합계
                </TableCell>
                <TableCell sx={{ textAlign: 'center', fontWeight: 600 }}>
                    {totalData.quantity.toLocaleString()}
                </TableCell>
                <TableCell sx={{ textAlign: 'center', fontWeight: 600 }}>
                    {totalData.unitPrice.toLocaleString()}원
                </TableCell>
                <TableCell sx={{ textAlign: 'center', fontWeight: 600 }}>
                    {totalData.totalPrice.toLocaleString()}원
                </TableCell>
                <TableCell sx={{ textAlign: 'center', fontWeight: 600 }}>
                    {totalData.deduction.toLocaleString()}원
                </TableCell>
                <TableCell sx={{ textAlign: 'center', fontWeight: 600 }}>
                    {totalData.paymentAmount.toLocaleString()}원
                </TableCell>
                <TableCell colSpan={2} sx={{ backgroundColor: palette.grey[50] }} />
            </TableRow>
        );
    };

    return (
        <>
            <Stack>
                <Title title={t('수매 목록')}>
                    {role !== 'VILLAGE_HEAD' && (
                        <Button
                            variant="outlinedBlue"
                            onClick={() => setOpenCreateDialog(true)}
                            startIcon={<Plus />}
                        >
                            추가
                        </Button>
                    )}
                </Title>
                <PageLayout gap="20px">
                    {/* 필터 영역 */}
                    <Stack
                        direction="row"
                        gap="8px"
                        alignItems="center"
                        justifyContent="flex-start"
                    >
                        <CustomDatePicker
                            value={startDate}
                            onChange={(value) => setStartDate(value ?? undefined)}
                            sx={{ width: '200px' }}
                        />
                        <Typography color={palette.grey[500]}>~</Typography>
                        <CustomDatePicker
                            value={endDate}
                            onChange={(value) => setEndDate(value ?? undefined)}
                            sx={{ width: '200px' }}
                        />

                        <Stack sx={{ width: '150px' }}>
                            <Select
                                autoWidth
                                value={villageHeadId}
                                onChange={(e) => setVillageHeadId(e.target.value)}
                                displayEmpty
                                renderValue={(selected) => {
                                    if (!selected) {
                                        return (
                                            <Typography color="text.disabled">면장 선택</Typography>
                                        );
                                    }
                                    return (
                                        <Typography color={palette.common.black}>
                                            {
                                                villageHeadList?.find(
                                                    (head) => head.id === Number(selected)
                                                )?.appUserName
                                            }
                                        </Typography>
                                    );
                                }}
                            >
                                {villageHeadList?.map((head) => (
                                    <MenuItem key={head.id} value={head.id}>
                                        {head.appUserName}
                                    </MenuItem>
                                ))}
                            </Select>
                        </Stack>
                    </Stack>
                    <Table
                        headData={
                            role !== 'VILLAGE_HEAD'
                                ? [
                                      t('ID'),
                                      t('담당자'),
                                      t('면장'),
                                      t('거래일자'),
                                      t('수량'),
                                      t('단가'),
                                      t('총액'),
                                      t('차감액'),
                                      t('지급액'),
                                      t('비고'),
                                      '',
                                  ]
                                : [
                                      t('ID'),
                                      t('담당자'),
                                      t('면장'),
                                      t('거래일자'),
                                      t('수량'),
                                      t('단가'),
                                      t('총액'),
                                      t('차감액'),
                                      t('지급액'),
                                      t('비고'),
                                  ]
                        }
                        bodyData={purchaseList || []}
                        renderRow={renderRow}
                        tableFooter={totalRow()}
                    />
                </PageLayout>
            </Stack>

            <TreesPurchaseCreateDialog
                open={openCreateDialog}
                onClose={() => setOpenCreateDialog(false)}
            />

            {selectedPurchaseDetail && (
                <TreesPurchaseEditDialog
                    open={openEditDialog}
                    onClose={() => {
                        setOpenEditDialog(false);
                        setSelectedPurchaseDetail(null);
                    }}
                    purchaseDetail={selectedPurchaseDetail}
                />
            )}
        </>
    );
};

export default TreesPurchaseList;
