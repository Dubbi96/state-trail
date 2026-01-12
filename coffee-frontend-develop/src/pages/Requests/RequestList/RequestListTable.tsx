import { useFormContext } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { DeleteOutline } from '@mui/icons-material';
import { Chip, IconButton, Pagination, Stack, TableCell, TableRow } from '@mui/material';
import dayjs from 'dayjs';

import { getCookies } from '@/apis/AppUser/cookie';
import { useDeleteApproval } from '@/apis/Approval/useDeleteApproval';
import Table from '@/components/Table';
import { useDialog } from '@/hooks/useDialog';
import { palette } from '@/themes';
import { TRequestListRes, TRequestListTableRow } from '@/typings/Requests';
import { showToast } from '@/utils/showToast';

import { REQUEST_METHOD, REQUEST_SERVICE_TYPE, REQUEST_STATUS } from '../constants';

type RequestListTableProps = {
    data: TRequestListRes;
    isLoading: boolean;
};

const RequestListTable = ({ data, isLoading }: RequestListTableProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { openDialog } = useDialog();
    const appUserId = getCookies('appUserId');
    const { watch, setValue } = useFormContext();

    const { mutateAsync: deleteApproval } = useDeleteApproval();
    const handleDeleteApproval = (approvalId: number) => {
        openDialog({
            title: t('요청 삭제'),
            description: t('삭제된 요청은 되돌릴 수 없습니다.'),
            variant: 'alert',
            primaryAction: {
                name: t('확인'),
                onClick: async () => {
                    const res = await deleteApproval({ approvalId });
                    if (res.code === 'SUCCESS') {
                        showToast.success(res.message);
                    } else showToast.error(res.message);
                },
            },
            secondaryAction: {
                name: t('취소'),
                onClick: () => { },
            },
        });
    };

    const handleChangePage = (_: React.ChangeEvent<unknown>, page: number) => {
        setValue('pageable.page', page - 1);
    };

    const renderRow = (row: TRequestListTableRow) => {
        return (
            <TableRow
                key={row.id}
                onClick={() => navigate(`${row.serviceType.toLocaleLowerCase()}/${row.id}`)}
            >
                <TableCell>
                    {row.id.toString()}
                </TableCell>
                <TableCell>
                    {REQUEST_SERVICE_TYPE[row.serviceType]} / {REQUEST_METHOD[row.method]}
                </TableCell>
                <TableCell>{dayjs(row.createdAt).format('YYYY.MM.DD HH:mm')}</TableCell>
                <TableCell>{row.requesterName}</TableCell>
                <TableCell>
                    <Chip
                        label={REQUEST_STATUS[row.status].label}
                        color={REQUEST_STATUS[row.status].color as 'blue' | 'yellow' | 'red'}
                    />
                </TableCell>
                <TableCell>
                    {row.status === 'PENDING' && row.requesterId === appUserId && (
                        <IconButton
                            aria-label="delete"
                            size="small"
                            onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteApproval(row.id);
                            }}
                        >
                            <DeleteOutline sx={{ color: palette.grey[500] }} />
                        </IconButton>
                    )}
                </TableCell>
            </TableRow>
        );
    };

    return (
        <>
            <Table
                headData={[t('ID'), t('요청 분류'), t('요청 일시'), t('담당자'), t('요청 상태'), '']}
                bodyData={data?.content}
                renderRow={renderRow}
                isLoading={isLoading}
            />
            <Stack
                sx={{
                    alignItems: 'center',
                }}
            >
                <Pagination
                    count={data?.totalPages}
                    page={watch('pageable.page') + 1}
                    onChange={handleChangePage}
                />
            </Stack>
        </>
    );
};

export default RequestListTable;
