import { FieldValues, Path, RegisterOptions, UseFormRegister } from 'react-hook-form';

import { StackProps, TextField } from '@mui/material';

import LabelComponentsLayout from '@/components/LabelComponentsLayout';

type LabelAndInputProps<T extends FieldValues> = StackProps & {
    disabled?: boolean;
    register: UseFormRegister<T>;
    rules?: RegisterOptions<T, Path<T>>;
    labelValue: string;
    fieldName: Path<T>;
    placeholder?: string;
    type?: string;
};

const LabelAndInput = <T extends FieldValues>({
    register,
    rules,
    labelValue,
    fieldName,
    placeholder = '',
    type = 'text',
    disabled = false,
    ...props
}: LabelAndInputProps<T>) => {
    return (
        <LabelComponentsLayout {...props} labelValue={labelValue} sx={{ ...props.sx }}>
            <TextField
                {...register(fieldName, rules)}
                disabled={disabled}
                placeholder={placeholder}
                slotProps={{ htmlInput: { type } }}
            />
        </LabelComponentsLayout>
    );
};

export default LabelAndInput;
