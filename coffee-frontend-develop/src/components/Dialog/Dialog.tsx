import { Check, ErrorOutline } from '@mui/icons-material';
import {
    Button,
    DialogActions,
    DialogTitle,
    Dialog as MuiDialog,
    Stack,
    Typography,
} from '@mui/material';

import { palette } from '@/themes';

type ActionType = {
    name: string;
    onClick: () => void;
};

export type DialogProps = {
    open: boolean;
    onClose: () => void;
    title: string;
    description?: string;
    primaryAction?: ActionType;
    secondaryAction?: ActionType;
    variant: 'confirm' | 'alert';
};

const Dialog = ({
    open,
    onClose,
    title,
    description,
    primaryAction,
    secondaryAction,
    variant,
}: DialogProps) => {
    return (
        <MuiDialog
            open={open}
            onClose={onClose}
            sx={{
                '& .MuiDialog-container': {
                    '& .MuiPaper-root': {
                        width: '500px',
                        borderRadius: '8px',
                    },
                },
            }}
        >
            <DialogTitle
                sx={{ display: 'flex', flexDirection: 'row', padding: '24px', gap: '10px' }}
            >
                <Stack
                    sx={{
                        justifyContent: 'center',
                        alignItems: 'center',
                        backgroundColor: variant === 'confirm' ? palette.blue[50] : palette.red[50],
                        width: '32px',
                        height: '32px',
                        borderRadius: '100%',
                    }}
                >
                    {variant === 'confirm' && (
                        <Check sx={{ color: palette.blue[600], width: '20px', height: '20px' }} />
                    )}
                    {variant === 'alert' && (
                        <ErrorOutline
                            sx={{ color: palette.red.main, width: '20px', height: '20px' }}
                        />
                    )}
                </Stack>
                <Stack sx={{ gap: '10px' }}>
                    <Typography
                        sx={{ color: palette.grey[800], fontSize: '18px', fontWeight: '500' }}
                    >
                        {title}
                    </Typography>
                    <Typography sx={{ color: palette.grey[600], whiteSpace: 'pre-line' }}>
                        {description}
                    </Typography>
                </Stack>
            </DialogTitle>
            <DialogActions
                sx={{
                    'p': '16px 24px',
                    '& > button': { height: '38px', width: 'content-fit', padding: '0 16px' },
                }}
            >
                {secondaryAction && (
                    <Button
                        variant="containedGrey"
                        onClick={() => {
                            secondaryAction.onClick();
                            onClose();
                        }}
                    >
                        {secondaryAction.name}
                    </Button>
                )}
                {primaryAction && (
                    <Button
                        variant={variant === 'confirm' ? 'containedBlue' : 'containedRed'}
                        onClick={() => {
                            primaryAction.onClick();
                            onClose();
                        }}
                    >
                        {primaryAction.name}
                    </Button>
                )}
            </DialogActions>
        </MuiDialog>
    );
};

export default Dialog;
