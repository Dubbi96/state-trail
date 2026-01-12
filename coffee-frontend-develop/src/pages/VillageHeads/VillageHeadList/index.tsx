import { MouseEvent, useState } from 'react';

import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { MoreHoriz } from '@mui/icons-material';
import {
    Button,
    Divider,
    IconButton,
    Menu,
    MenuItem,
    Stack,
    TableCell,
    TableRow,
} from '@mui/material';
import { Plus } from '@phosphor-icons/react';

import { GetVillageHeadListRes, useGetVillageHeadList } from '@/apis/AppUser/useGetVillageHeadList';
import { useDeleteVillageHead } from '@/apis/Approval/useDeleteVillageHead';
import PageLayout from '@/components/PageLayout';
import Table from '@/components/Table';
import Title from '@/components/Title';
import { useDialog } from '@/hooks/useDialog';

import CreateVillageHeadDialog from './create-dialog';
import EditVillageHeadDialog from './edit-dialog';

const VillageHeadList = () => {
    const { openDialog } = useDialog();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { data } = useGetVillageHeadList();
    const { mutate: deleteVillageHead } = useDeleteVillageHead();
    const [openCreateDialog, setOpenCreateDialog] = useState(false);
    const [openEditDialog, setOpenEditDialog] = useState(false);
    const [selectedId, setSelectedId] = useState<number | null>(null);

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
            title: t('해당 면장을 삭제하시겠습니까?'),
            description: t('삭제하면 복구가 불가능합니다.'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: () => {
                    deleteVillageHead({ villageHeadId: id });

                    handleCloseMenu();
                },
            },
            secondaryAction: { name: t('취소'), onClick: () => {} },
        });
    };
    const renderRow = (row: GetVillageHeadListRes) => {
        return (
            <TableRow key={row.id} onClick={() => navigate(`/village-heads/${row.id}`)}>
                <TableCell>{row.id.toString()}</TableCell>
                <TableCell>{row.appUserId}</TableCell>
                <TableCell>{row.appUserName}</TableCell>
                <TableCell>
                    <Button
                        variant="text"
                        onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/locations/area/${row.areaInfo.areaId}`);
                        }}
                    >
                        {row.areaInfo.areaName}
                    </Button>
                </TableCell>
                <TableCell>
                    <Button
                        variant="text"
                        onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/locations/section/${row.sectionInfo.sectionId}`);
                        }}
                    >
                        {row.sectionInfo.sectionName}
                    </Button>
                </TableCell>
                <TableCell>{row.farmerCount}</TableCell>
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
                                setSelectedId(row.id);
                                handleCloseMenu();
                            }}
                        >
                            수정하기
                        </MenuItem>
                        <Divider />
                        <MenuItem
                            onClick={() => {
                                handleClickDelete(row.id);
                                handleCloseMenu();
                            }}
                        >
                            삭제하기
                        </MenuItem>
                    </Menu>
                </TableCell>
            </TableRow>
        );
    };

    return (
        <Stack sx={{ width: '100%' }}>
            <Title title={t('면장 목록')}>
                <Button
                    variant="outlinedBlue"
                    onClick={() => {
                        setOpenCreateDialog(true);
                    }}
                    startIcon={<Plus />}
                >
                    {t('등록')}
                </Button>
            </Title>
            <PageLayout>
                <Table
                    headData={[
                        t('ID'),
                        t('아이디'),
                        t('이름'),
                        t('지역'),
                        t('섹션'),
                        t('관리 농부 수'),
                        '',
                    ]}
                    bodyData={data || undefined}
                    renderRow={renderRow}
                />
            </PageLayout>

            <CreateVillageHeadDialog
                open={openCreateDialog}
                onClose={() => setOpenCreateDialog(false)}
            />

            {selectedId && (
                <EditVillageHeadDialog
                    open={openEditDialog}
                    onClose={() => {
                        setOpenEditDialog(false);
                        setSelectedId(null);
                    }}
                    id={String(selectedId)}
                />
            )}
        </Stack>
    );
};

export default VillageHeadList;
